import { useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { Circle, CircleMarker, Popup } from 'react-leaflet'
import { api, consulta } from '../api'
import { useSesion } from '../sesion'
import { useUbicacion } from '../useUbicacion'
import { useEvento } from '../tiempoReal'
import { fecha, NIVELES, TIPOS } from '../catalogos'
import MapaBase, { COLOR_NIVEL } from '../componentes/MapaBase'
import BotonPanico from '../componentes/BotonPanico'
import type { Reporte, RespuestaHotspots, Riesgo } from '../tipos'

const PERIODOS = [
  { dias: 7, texto: '7 días' },
  { dias: 30, texto: '30 días' },
  { dias: 90, texto: '90 días' },
]

export default function Mapa() {
  const { usuario } = useSesion()
  const ubicacion = useUbicacion()
  const [dias, setDias] = useState(30)
  const [verZonas, setVerZonas] = useState(true)
  const [soloCerca, setSoloCerca] = useState(false)
  const [reportes, setReportes] = useState<Reporte[]>([])
  const [zonas, setZonas] = useState<RespuestaHotspots | null>(null)
  const [riesgo, setRiesgo] = useState<Riesgo | null>(null)
  const [error, setError] = useState('')
  const [recarga, setRecarga] = useState(0)
  const ultimaUbicacion = useRef(0)

  // HU 02: la app informa la ubicación real (como máximo una vez por minuto) para saber a quién alertar.
  useEffect(() => {
    if (!ubicacion.real || usuario?.rol !== 'CIUDADANO') return
    if (Date.now() - ultimaUbicacion.current < 60000) return
    ultimaUbicacion.current = Date.now()
    api.put('/auth/yo/ubicacion', { latitud: ubicacion.lat, longitud: ubicacion.lng }).catch(() => {})
  }, [ubicacion.real, ubicacion.lat, ubicacion.lng, usuario?.rol])

  // Un reporte nuevo de cualquier vecino aparece en el mapa sin recargar.
  useEvento('reporte', () => setRecarga((n) => n + 1))

  const desde = useMemo(() => new Date(Date.now() - dias * 86400000).toISOString(), [dias])

  useEffect(() => {
    setError('')
    const filtro = soloCerca ? { lat: ubicacion.lat, lng: ubicacion.lng, radio: 200 } : {}
    api
      .get<Reporte[]>(
        `/reportes${consulta({ desde, estado: 'PENDIENTE,VALIDADO,EN_REVISION', ...filtro })}`,
      )
      .then(setReportes)
      .catch((e) => setError(e.message))
  }, [desde, soloCerca, ubicacion.lat, ubicacion.lng, recarga])

  useEffect(() => {
    api.get<RespuestaHotspots>(`/analitica/hotspots${consulta({ dias: Math.max(dias, 30) })}`).then(setZonas).catch(() => {})
  }, [dias])

  useEffect(() => {
    api.get<Riesgo>(`/analitica/riesgo${consulta({ lat: ubicacion.lat, lng: ubicacion.lng })}`).then(setRiesgo).catch(() => {})
  }, [ubicacion.lat, ubicacion.lng])

  const nombre = usuario?.nombre.split(' ')[0]

  return (
    <div className="mapa-pantalla">
      <MapaBase centro={[ubicacion.lat, ubicacion.lng]} zoom={15}>
        {verZonas &&
          zonas?.hotspots.map((z) => (
            <Circle
              key={`z${z.id}`}
              center={[z.lat, z.lng]}
              radius={z.radioMetros}
              pathOptions={{ color: COLOR_NIVEL[z.nivel], weight: 2, fillOpacity: 0.18 }}
            >
              <Popup>
                <strong>Punto caliente · riesgo {NIVELES[z.nivel].toLowerCase()}</strong>
                <br />
                {z.reportes} reportes, más frecuentes entre {z.franjaPico}.
              </Popup>
            </Circle>
          ))}
        {soloCerca && (
          <Circle
            center={[ubicacion.lat, ubicacion.lng]}
            radius={200}
            pathOptions={{ color: '#1d3b53', weight: 1.5, dashArray: '4 6', fillOpacity: 0.04 }}
          />
        )}
        {reportes.map((r) => (
          <CircleMarker
            key={r.id}
            center={[r.latitud, r.longitud]}
            radius={7}
            pathOptions={{ color: '#ffffff', weight: 2, fillColor: COLOR_NIVEL[r.severidad], fillOpacity: 1 }}
          >
            <Popup>
              <strong>{TIPOS[r.tipoDelito]}</strong>
              <br />
              {fecha(r.fechaHecho)}
              {r.barrio ? ` · ${r.barrio}` : ''}
              <br />
              <Link to={`/reportes/${r.id}`}>Ver detalle y validar</Link>
            </Popup>
          </CircleMarker>
        ))}
        <CircleMarker
          center={[ubicacion.lat, ubicacion.lng]}
          radius={8}
          pathOptions={{ color: '#ffffff', weight: 3, fillColor: '#1d6fd1', fillOpacity: 1 }}
        >
          <Popup>{ubicacion.real ? 'Tu ubicación' : 'Ubicación aproximada: activa el GPS para mayor precisión'}</Popup>
        </CircleMarker>
      </MapaBase>

      <div className="mapa-cabecera">
        <div className="tarjeta-flotante fila entre">
          <div>
            <h1 style={{ fontSize: '1.125rem' }}>Hola, {nombre}</h1>
            <p className="small muted">{riesgo ? riesgo.explicacion : 'Calculando el riesgo de tu zona…'}</p>
          </div>
          {riesgo && (
            <span className={`riesgo ${riesgo.nivel}`} title={`Índice ${riesgo.indice} de 100`}>
              Riesgo {NIVELES[riesgo.nivel].toLowerCase()}
            </span>
          )}
        </div>
        <div className="chips" role="toolbar" aria-label="Filtros del mapa">
          {PERIODOS.map((p) => (
            <button key={p.dias} className="chip" aria-pressed={dias === p.dias} onClick={() => setDias(p.dias)}>
              {p.texto}
            </button>
          ))}
          <button className="chip" aria-pressed={verZonas} onClick={() => setVerZonas((v) => !v)}>
            Puntos calientes
          </button>
          <button className="chip" aria-pressed={soloCerca} onClick={() => setSoloCerca((v) => !v)}>
            A 200 m de mí
          </button>
        </div>
        {error && (
          <p className="alerta-error" role="alert">
            {error}
          </p>
        )}
      </div>

      <div className="leyenda tarjeta-flotante" aria-label="Leyenda">
        <span>
          <i style={{ background: COLOR_NIVEL.ALTO }} /> Riesgo alto
        </span>
        <span>
          <i style={{ background: COLOR_NIVEL.MEDIO }} /> Riesgo medio
        </span>
        <span>
          <i style={{ background: COLOR_NIVEL.BAJO }} /> Riesgo bajo
        </span>
        <span className="muted num">{reportes.length} reportes en el mapa</span>
      </div>

      {usuario?.rol === 'CIUDADANO' && <BotonPanico ubicacion={ubicacion} />}
    </div>
  )
}
