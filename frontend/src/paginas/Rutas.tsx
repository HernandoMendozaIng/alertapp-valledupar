import { useCallback, useEffect, useRef, useState } from 'react'
import { CircleMarker, Polyline, Popup, Tooltip, useMap, useMapEvents } from 'react-leaflet'
import { latLngBounds } from 'leaflet'
import { Bicycle, Car, FloppyDisk, NavigationArrow, PersonSimpleWalk, ShareNetwork, Trash, X } from '@phosphor-icons/react'
import { api, consulta, ErrorApi } from '../api'
import { useAviso } from '../aviso'
import { useUbicacion } from '../useUbicacion'
import { distancia, metros, MODOS, NIVELES } from '../catalogos'
import { useEvento } from '../tiempoReal'
import { restante, useSeguimiento } from '../seguimiento'
import MapaBase, { COLOR_NIVEL } from '../componentes/MapaBase'
import type { Modo, Reporte, ResultadoRuta, RutaFrecuente } from '../tipos'

type Punto = { lat: number; lng: number }

const ICONOS: Record<Modo, typeof PersonSimpleWalk> = {
  A_PIE: PersonSimpleWalk,
  BICICLETA: Bicycle,
  VEHICULO: Car,
}

function SeleccionEnMapa({ alTocar }: { alTocar: (p: Punto) => void }) {
  useMapEvents({ click: (e) => alTocar({ lat: e.latlng.lat, lng: e.latlng.lng }) })
  return null
}

/**
 * Encuadra la ruta en la parte del mapa que no tapan la tarjeta de arriba ni el panel del resultado.
 */
function Encuadrar({ resultado }: { resultado: ResultadoRuta | null }) {
  const mapa = useMap()
  useEffect(() => {
    if (!resultado) return
    const puntos = [...resultado.segura.puntos, ...resultado.corta.puntos]
    const caja = mapa.getContainer().getBoundingClientRect()
    const arriba = document.querySelector('.mapa-cabecera')?.getBoundingClientRect()
    const panel = document.querySelector('.panel-ruta')?.getBoundingClientRect()
    const escritorio = window.innerWidth >= 900
    const top = arriba && !escritorio ? arriba.bottom - caja.top + 16 : 24
    const abajo = panel && !escritorio ? caja.bottom - panel.top + 16 : 24
    const derecha = panel && escritorio ? caja.right - panel.left + 24 : 24
    const izquierda = arriba && escritorio ? arriba.right - caja.left + 24 : 24
    mapa.fitBounds(latLngBounds(puntos), {
      paddingTopLeft: [izquierda, top],
      paddingBottomRight: [derecha, abajo],
      maxZoom: 17,
      animate: true,
    })
  }, [mapa, resultado])
  return null
}

/**
 * HU 04 - Rutas seguras y HU 06 - Rutas frecuentes y modo seguimiento.
 * El usuario marca origen y destino, elige cómo se mueve y ve la ruta segura junto a la más corta.
 * Si entra un reporte nuevo cerca de la ruta, se recalcula sola.
 */
export default function Rutas() {
  const aviso = useAviso()
  const ubicacion = useUbicacion()
  const seguimiento = useSeguimiento()
  const [origen, setOrigen] = useState<Punto | null>(null)
  const [destino, setDestino] = useState<Punto | null>(null)
  const [marcando, setMarcando] = useState<'origen' | 'destino'>('destino')
  const [modo, setModo] = useState<Modo>('A_PIE')
  const [resultado, setResultado] = useState<ResultadoRuta | null>(null)
  const [calculando, setCalculando] = useState(false)
  const [error, setError] = useState('')
  const [frecuentes, setFrecuentes] = useState<RutaFrecuente[]>([])
  const [rutaElegida, setRutaElegida] = useState<RutaFrecuente | null>(null)
  const [reportes, setReportes] = useState<Reporte[]>([])
  const [minutos, setMinutos] = useState(30)
  const [recalculada, setRecalculada] = useState(false)
  const pedido = useRef(0)

  const desde = origen ?? { lat: ubicacion.lat, lng: ubicacion.lng }

  useEffect(() => {
    api.get<RutaFrecuente[]>('/rutas-frecuentes').then(setFrecuentes).catch(() => {})
    const hace90 = new Date(Date.now() - 90 * 86400000).toISOString()
    api
      .get<Reporte[]>(`/reportes${consulta({ desde: hace90, estado: 'PENDIENTE,VALIDADO,EN_REVISION' })}`)
      .then(setReportes)
      .catch(() => {})
  }, [])

  const calcular = useCallback(
    async (o: Punto, d: Punto, m: Modo) => {
      const n = ++pedido.current
      setCalculando(true)
      setError('')
      try {
        const r = await api.get<ResultadoRuta>(
          `/rutas/segura${consulta({ origenLat: o.lat, origenLng: o.lng, destinoLat: d.lat, destinoLng: d.lng, modo: m })}`,
        )
        if (n === pedido.current) setResultado(r)
        return r
      } catch (e) {
        if (n === pedido.current) {
          setResultado(null)
          setError(e instanceof ErrorApi ? e.message : 'No se pudo calcular la ruta')
        }
        return null
      } finally {
        if (n === pedido.current) setCalculando(false)
      }
    },
    [],
  )

  // Criterio de la HU 04: recálculo automático cuando entra un reporte cerca de la ruta.
  useEvento('reporte', (datos) => {
    const r = datos as { latitud: number; longitud: number }
    if (!resultado || !destino) return
    const cerca = resultado.segura.puntos.some(([la, ln]) => distancia(la, ln, r.latitud, r.longitud) < 250)
    if (!cerca) return
    calcular(desde, destino, modo).then((nuevo) => {
      if (nuevo) {
        setRecalculada(true)
        aviso('Hubo un reporte nuevo cerca de tu ruta. La recalculé.', 'error')
      }
    })
  })

  function tocarMapa(p: Punto) {
    setRutaElegida(null)
    setRecalculada(false)
    if (marcando === 'origen') {
      setOrigen(p)
      setMarcando('destino')
      if (destino) calcular(p, destino, modo)
    } else {
      setDestino(p)
      calcular(desde, p, modo)
    }
  }

  function cambiarModo(m: Modo) {
    setModo(m)
    if (destino) calcular(desde, destino, m)
  }

  function usarFrecuente(r: RutaFrecuente) {
    const o = { lat: r.origenLatitud, lng: r.origenLongitud }
    const d = { lat: r.destinoLatitud, lng: r.destinoLongitud }
    setRutaElegida(r)
    setOrigen(o)
    setDestino(d)
    setModo(r.modo)
    setRecalculada(false)
    calcular(o, d, r.modo)
  }

  function limpiar() {
    setOrigen(null)
    setDestino(null)
    setResultado(null)
    setRutaElegida(null)
    setError('')
    setMarcando('destino')
  }

  return (
    <div className="mapa-pantalla">
      <MapaBase centro={[desde.lat, desde.lng]} zoom={15}>
        <SeleccionEnMapa alTocar={tocarMapa} />
        <Encuadrar resultado={resultado} />
        {reportes.map((r) => (
          <CircleMarker
            key={r.id}
            center={[r.latitud, r.longitud]}
            radius={4}
            pathOptions={{ color: COLOR_NIVEL[r.severidad], weight: 0, fillColor: COLOR_NIVEL[r.severidad], fillOpacity: 0.55 }}
          />
        ))}
        {resultado && !resultado.mismaRuta && (
          <Polyline
            positions={resultado.corta.puntos}
            pathOptions={{ color: '#6b737b', weight: 5, opacity: 0.8, dashArray: '2 10', lineCap: 'round' }}
          >
            <Tooltip sticky>Ruta más corta · pasa cerca de {resultado.corta.reportesCercanos} reportes</Tooltip>
          </Polyline>
        )}
        {resultado && (
          <>
            <Polyline positions={resultado.segura.puntos} pathOptions={{ color: '#ffffff', weight: 10, opacity: 0.9 }} />
            <Polyline positions={resultado.segura.puntos} pathOptions={{ color: '#1d3b53', weight: 6, opacity: 1 }}>
              <Tooltip sticky>Ruta segura</Tooltip>
            </Polyline>
          </>
        )}
        <CircleMarker
          center={[desde.lat, desde.lng]}
          radius={9}
          pathOptions={{ color: '#ffffff', weight: 3, fillColor: '#1d6fd1', fillOpacity: 1 }}
        >
          <Popup>{origen ? 'Origen' : 'Tu ubicación (origen)'}</Popup>
        </CircleMarker>
        {destino && (
          <CircleMarker
            center={[destino.lat, destino.lng]}
            radius={9}
            pathOptions={{ color: '#ffffff', weight: 3, fillColor: '#15191d', fillOpacity: 1 }}
          >
            <Popup>Destino</Popup>
          </CircleMarker>
        )}
      </MapaBase>

      <div className="mapa-cabecera estrecha">
        <div className="tarjeta-flotante" style={{ display: 'grid', gap: 10 }}>
          <div className="fila entre">
            <h1 style={{ fontSize: '1.125rem' }}>Ruta segura</h1>
            {(destino || origen) && (
              <button className="btn texto chico" onClick={limpiar}>
                <X size={16} weight="bold" aria-hidden /> Limpiar
              </button>
            )}
          </div>
          <div className="puntos-ruta" role="group" aria-label="Qué punto marcas al tocar el mapa">
            <button aria-pressed={marcando === 'origen'} onClick={() => setMarcando('origen')}>
              <span className="punto origen" aria-hidden />
              {origen ? 'Origen marcado' : ubicacion.real ? 'Desde mi ubicación' : 'Desde el centro (sin GPS)'}
            </button>
            <button aria-pressed={marcando === 'destino'} onClick={() => setMarcando('destino')}>
              <span className="punto destino" aria-hidden />
              {destino ? 'Destino marcado' : 'Toca el mapa para el destino'}
            </button>
          </div>
          <div className="chips" role="group" aria-label="Cómo te desplazas">
            {(Object.keys(MODOS) as Modo[]).map((m) => {
              const Icono = ICONOS[m]
              return (
                <button key={m} className="chip" aria-pressed={modo === m} onClick={() => cambiarModo(m)}>
                  <Icono size={16} weight="bold" aria-hidden /> {MODOS[m]}
                </button>
              )
            })}
          </div>
          {frecuentes.length > 0 && (
            <div className="chips" aria-label="Mis rutas frecuentes">
              {frecuentes.map((r) => (
                <button key={r.id} className="chip" aria-pressed={rutaElegida?.id === r.id} onClick={() => usarFrecuente(r)}>
                  <NavigationArrow size={14} weight="bold" aria-hidden /> {r.nombre}
                </button>
              ))}
            </div>
          )}
          {error && (
            <p className="alerta-error" role="alert">
              {error}
            </p>
          )}
        </div>
      </div>

      {seguimiento.actual && (
        <div className="banda-seguimiento" role="status">
          <span className="pulso" aria-hidden />
          <span>
            Compartiendo tu ubicación{seguimiento.actual.rutaNombre ? ` · ${seguimiento.actual.rutaNombre}` : ''} ·{' '}
            {restante(seguimiento.actual.expiraEn)}
          </span>
          <button className="btn chico secundario" onClick={seguimiento.detener}>
            Detener
          </button>
        </div>
      )}

      {(resultado || calculando) && (
        <PanelResultado
          resultado={resultado}
          calculando={calculando}
          recalculada={recalculada}
          modo={modo}
          minutos={minutos}
          setMinutos={setMinutos}
          compartiendo={Boolean(seguimiento.actual)}
          compartir={() => seguimiento.iniciar(minutos, desde.lat, desde.lng, rutaElegida?.id ?? null)}
          rutaElegida={rutaElegida}
          guardar={async (nombre) => {
            if (!destino) return
            try {
              const nueva = await api.post<RutaFrecuente>('/rutas-frecuentes', {
                nombre,
                origenLatitud: desde.lat,
                origenLongitud: desde.lng,
                destinoLatitud: destino.lat,
                destinoLongitud: destino.lng,
                modo,
              })
              setFrecuentes((l) => [...l, nueva].sort((a, b) => a.nombre.localeCompare(b.nombre)))
              setRutaElegida(nueva)
              aviso('Ruta guardada')
            } catch (e) {
              aviso((e as Error).message, 'error')
            }
          }}
          renombrar={async (nombre) => {
            if (!rutaElegida) return
            try {
              const r = await api.put<RutaFrecuente>(`/rutas-frecuentes/${rutaElegida.id}`, { ...rutaElegida, nombre, modo })
              setFrecuentes((l) => l.map((x) => (x.id === r.id ? r : x)))
              setRutaElegida(r)
              aviso('Ruta actualizada')
            } catch (e) {
              aviso((e as Error).message, 'error')
            }
          }}
          borrar={async () => {
            if (!rutaElegida) return
            await api.delete(`/rutas-frecuentes/${rutaElegida.id}`).catch(() => {})
            setFrecuentes((l) => l.filter((x) => x.id !== rutaElegida.id))
            setRutaElegida(null)
            aviso('Ruta borrada')
          }}
        />
      )}
    </div>
  )
}

function PanelResultado({
  resultado,
  calculando,
  recalculada,
  modo,
  minutos,
  setMinutos,
  compartiendo,
  compartir,
  rutaElegida,
  guardar,
  renombrar,
  borrar,
}: {
  resultado: ResultadoRuta | null
  calculando: boolean
  recalculada: boolean
  modo: Modo
  minutos: number
  setMinutos: (m: number) => void
  compartiendo: boolean
  compartir: () => void
  rutaElegida: RutaFrecuente | null
  guardar: (nombre: string) => void
  renombrar: (nombre: string) => void
  borrar: () => void
}) {
  const [nombre, setNombre] = useState(rutaElegida?.nombre ?? '')
  const [abierto, setAbierto] = useState(false)
  useEffect(() => setNombre(rutaElegida?.nombre ?? ''), [rutaElegida])

  if (!resultado) {
    return (
      <section className="panel-ruta tarjeta-flotante" aria-busy="true">
        <div className="esqueleto" style={{ height: 96 }} />
      </section>
    )
  }
  const { segura, corta } = resultado
  return (
    <section className="panel-ruta tarjeta-flotante" aria-labelledby="titulo-ruta" aria-busy={calculando}>
      <div className="fila entre">
        <h2 id="titulo-ruta" style={{ fontSize: '1rem' }}>
          {MODOS[modo]} · {metros(segura.metros)} · {segura.minutos} min
        </h2>
        <span className={`riesgo ${segura.nivel}`}>Riesgo {NIVELES[segura.nivel].toLowerCase()}</span>
      </div>
      {recalculada && <p className="small alerta-suave">Ruta recalculada por un reporte nuevo.</p>}
      <p className="small">{resultado.explicacion}</p>
      {!resultado.mismaRuta && (
        <dl className="comparacion">
          <div>
            <dt>
              <i className="linea segura" aria-hidden /> Segura
            </dt>
            <dd className="num">
              {metros(segura.metros)} · {segura.reportesCercanos} {segura.reportesCercanos === 1 ? 'reporte' : 'reportes'} cerca
            </dd>
          </div>
          <div>
            <dt>
              <i className="linea corta" aria-hidden /> Más corta
            </dt>
            <dd className="num">
              {metros(corta.metros)} · {corta.reportesCercanos} {corta.reportesCercanos === 1 ? 'reporte' : 'reportes'} cerca
            </dd>
          </div>
        </dl>
      )}
      {segura.calles.length > 0 && <p className="small muted">Por {segura.calles.join(', ')}</p>}

      <div className="fila">
        {!compartiendo && (
          <>
            <label className="sr-only" htmlFor="ruta-minutos">
              Tiempo para compartir
            </label>
            <select id="ruta-minutos" className="chico" value={minutos} onChange={(e) => setMinutos(Number(e.target.value))}>
              <option value={15}>15 min</option>
              <option value={30}>30 min</option>
              <option value={60}>1 hora</option>
            </select>
            <button className="btn chico" onClick={compartir}>
              <ShareNetwork size={16} weight="bold" aria-hidden /> Compartir trayecto
            </button>
          </>
        )}
        <button className="btn chico secundario" onClick={() => setAbierto((a) => !a)} aria-expanded={abierto}>
          <FloppyDisk size={16} weight="bold" aria-hidden /> {rutaElegida ? 'Editar ruta' : 'Guardar ruta'}
        </button>
      </div>
      {abierto && (
        <form
          className="fila"
          onSubmit={(e) => {
            e.preventDefault()
            if (!nombre.trim()) return
            if (rutaElegida) renombrar(nombre.trim())
            else guardar(nombre.trim())
            setAbierto(false)
          }}
        >
          <label className="sr-only" htmlFor="ruta-nombre">
            Nombre de la ruta
          </label>
          <input
            id="ruta-nombre"
            placeholder="Ej.: Casa al trabajo"
            maxLength={60}
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            style={{ flex: 1, minWidth: 160 }}
          />
          <button className="btn chico" disabled={!nombre.trim()}>
            Guardar
          </button>
          {rutaElegida && (
            <button type="button" className="btn chico texto" onClick={borrar} aria-label={`Borrar ${rutaElegida.nombre}`}>
              <Trash size={16} weight="bold" aria-hidden /> Borrar
            </button>
          )}
        </form>
      )}
    </section>
  )
}
