import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Marker, useMapEvents } from 'react-leaflet'
import L from 'leaflet'
import {
  Bicycle,
  Camera,
  Car,
  Crosshair,
  CurrencyCircleDollar,
  DeviceMobile,
  Diamond,
  Eye,
  HandFist,
  Handbag,
  Knife,
  Motorcycle,
  PersonSimpleWalk,
  Prohibit,
  Question,
  TShirt,
  Wrench,
} from '@phosphor-icons/react'
import { api, ErrorApi } from '../api'
import { useAviso } from '../aviso'
import { ARMAS, BARRIOS, ELEMENTOS, MEDIOS, TIPOS } from '../catalogos'
import { comunaDe } from '../comunas'
import { useUbicacion } from '../useUbicacion'
import MapaBase from '../componentes/MapaBase'
import type { Arma, Elemento, Medio, Reporte, ReportePeticion, TipoDelito } from '../tipos'

const ICONOS_TIPO: Record<TipoDelito, ReactNode> = {
  ATRACO: <HandFist size={24} />,
  HURTO: <Handbag size={24} />,
  SOSPECHOSO: <Eye size={24} />,
  OTRO: <Question size={24} />,
}
const ICONOS_ARMA: Record<Arma, ReactNode> = {
  ARMA_FUEGO: <Crosshair size={24} />,
  CORTOPUNZANTE: <Knife size={24} />,
  SIN_ARMA: <Prohibit size={24} />,
  NO_IDENTIFICADA: <Question size={24} />,
}
const ICONOS_MEDIO: Record<Medio, ReactNode> = {
  A_PIE: <PersonSimpleWalk size={24} />,
  BICICLETA: <Bicycle size={24} />,
  MOTOCICLETA: <Motorcycle size={24} />,
  VEHICULO: <Car size={24} />,
  NO_IDENTIFICADO: <Question size={24} />,
}
const ICONOS_ELEMENTO: Record<Elemento, ReactNode> = {
  CELULAR: <DeviceMobile size={24} />,
  DINERO: <CurrencyCircleDollar size={24} />,
  JOYAS: <Diamond size={24} />,
  PRENDA: <TShirt size={24} />,
  BICICLETA: <Bicycle size={24} />,
  MOTOCICLETA: <Motorcycle size={24} />,
  VEHICULO: <Car size={24} />,
  PARTE_VEHICULO: <Wrench size={24} />,
  NINGUNO: <Prohibit size={24} />,
  OTRO: <Question size={24} />,
}

const pin = L.divIcon({
  className: '',
  html: '<svg width="36" height="44" viewBox="0 0 32 40"><path d="M16 1C8 1 2 7 2 14.5 2 25 16 39 16 39s14-14 14-24.5C30 7 24 1 16 1z" fill="#1d3b53" stroke="#fff" stroke-width="2"/><circle cx="16" cy="14.5" r="5" fill="#D32F2F"/></svg>',
  iconSize: [36, 44],
  iconAnchor: [18, 43],
})

function ahoraLocal() {
  const d = new Date(Date.now() - new Date().getTimezoneOffset() * 60000)
  return d.toISOString().slice(0, 16)
}

function aLocal(iso: string) {
  const d = new Date(iso)
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16)
}

function Selector({ onElegir }: { onElegir: (lat: number, lng: number) => void }) {
  useMapEvents({ click: (e) => onElegir(e.latlng.lat, e.latlng.lng) })
  return null
}

function Grupo<T extends string>({
  nombre,
  titulo,
  opciones,
  iconos,
  valor,
  onCambio,
  error,
}: {
  nombre: string
  titulo: string
  opciones: Record<T, string>
  iconos: Record<T, ReactNode>
  valor: T | ''
  onCambio: (v: T) => void
  error?: string
}) {
  return (
    <fieldset className="separado" style={{ gap: 10, border: 0, padding: 0, margin: 0 }}>
      <legend className="etiqueta" style={{ marginBottom: 10 }}>
        {titulo}
      </legend>
      <div className="opciones">
        {(Object.keys(opciones) as T[]).map((k) => (
          <label key={k} className="opcion">
            <input type="radio" name={nombre} value={k} checked={valor === k} onChange={() => onCambio(k)} />
            {iconos[k]}
            {opciones[k]}
          </label>
        ))}
      </div>
      {error && <span className="error small">{error}</span>}
    </fieldset>
  )
}

export default function Reportar() {
  const { id } = useParams()
  const editando = Boolean(id)
  const navegar = useNavigate()
  const aviso = useAviso()
  const ubicacion = useUbicacion()
  const [tipo, setTipo] = useState<TipoDelito | ''>('')
  const [arma, setArma] = useState<Arma | ''>('')
  const [medio, setMedio] = useState<Medio | ''>('')
  const [elemento, setElemento] = useState<Elemento | ''>('')
  const [fechaHecho, setFechaHecho] = useState(ahoraLocal())
  const [punto, setPunto] = useState<[number, number] | null>(null)
  const [comuna, setComuna] = useState<number | null>(null)
  const [barrio, setBarrio] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [anonimo, setAnonimo] = useState(true)
  const [foto, setFoto] = useState<File | null>(null)
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)
  const tocado = useRef(false)

  useEffect(() => {
    if (!id) return
    api
      .get<Reporte>(`/reportes/${id}`)
      .then((r) => {
        tocado.current = true
        setTipo(r.tipoDelito)
        setArma(r.arma)
        setMedio(r.medio)
        setElemento(r.elementoHurtado)
        setFechaHecho(aLocal(r.fechaHecho))
        setPunto([r.latitud, r.longitud])
        setComuna(r.comuna)
        setBarrio(r.barrio ?? '')
        setDescripcion(r.descripcion ?? '')
        setAnonimo(r.anonimo)
      })
      .catch((e) => setError(e.message))
  }, [id])

  // Por defecto el punto es la ubicación actual, hasta que el usuario toque el mapa.
  useEffect(() => {
    if (!tocado.current) setPunto([ubicacion.lat, ubicacion.lng])
  }, [ubicacion.lat, ubicacion.lng])

  useEffect(() => {
    if (!punto) return
    comunaDe(punto[0], punto[1]).then(setComuna)
  }, [punto])

  useEffect(() => {
    if (tipo === 'SOSPECHOSO') {
      setElemento('NINGUNO')
      if (!arma) setArma('SIN_ARMA')
    }
  }, [tipo]) // eslint-disable-line react-hooks/exhaustive-deps

  function validar() {
    const e: Record<string, string> = {}
    if (!tipo) e.tipoDelito = 'Elige qué pasó'
    if (!arma) e.arma = 'Indica el arma'
    if (!medio) e.medio = 'Indica cómo se movía'
    if (!elemento) e.elementoHurtado = 'Indica qué se llevaron'
    if (!punto) e.ubicacion = 'Marca el lugar en el mapa'
    if (punto && !comuna) e.ubicacion = 'El punto está fuera de las Comunas 3 y 4. Toca el mapa dentro del área punteada.'
    if (new Date(fechaHecho) > new Date()) e.fechaHecho = 'La fecha no puede ser futura'
    if (foto && foto.size > 5 * 1024 * 1024) e.foto = 'La foto supera 5 MB'
    setErrores(e)
    return Object.keys(e).length === 0
  }

  async function enviar(ev: FormEvent) {
    ev.preventDefault()
    setError('')
    if (!validar() || !punto || !comuna) {
      setError('Revisa los campos marcados')
      return
    }
    const datos: ReportePeticion = {
      tipoDelito: tipo as TipoDelito,
      arma: arma as Arma,
      medio: medio as Medio,
      elementoHurtado: elemento as Elemento,
      descripcion,
      fechaHecho: new Date(fechaHecho).toISOString(),
      latitud: Number(punto[0].toFixed(6)),
      longitud: Number(punto[1].toFixed(6)),
      comuna,
      barrio,
      anonimo,
    }
    setEnviando(true)
    try {
      let r: Reporte
      if (editando) {
        r = await api.put<Reporte>(`/reportes/${id}`, datos)
      } else if (foto) {
        const fd = new FormData()
        fd.append('datos', new Blob([JSON.stringify(datos)], { type: 'application/json' }))
        fd.append('foto', foto)
        r = await api.post<Reporte>('/reportes', fd)
      } else {
        r = await api.post<Reporte>('/reportes', datos)
      }
      aviso(editando ? 'Reporte actualizado' : 'Reporte enviado. Gracias por cuidar tu barrio.')
      navegar(`/reportes/${r.id}`, { replace: true })
    } catch (e) {
      if (e instanceof ErrorApi) {
        setErrores(e.campos)
        setError(e.message)
      }
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form className="pagina" onSubmit={enviar} noValidate>
      <div className="encabezado">
        <h1>{editando ? 'Editar reporte' : 'Reportar un hecho'}</h1>
        <p className="muted">Toma menos de un minuto. Tu nombre no se muestra si el reporte es anónimo.</p>
      </div>

      <Grupo nombre="tipo" titulo="¿Qué pasó?" opciones={TIPOS} iconos={ICONOS_TIPO} valor={tipo} onCambio={setTipo} error={errores.tipoDelito} />
      <Grupo nombre="arma" titulo="¿Usaron algún arma?" opciones={ARMAS} iconos={ICONOS_ARMA} valor={arma} onCambio={setArma} error={errores.arma} />
      <Grupo nombre="medio" titulo="¿Cómo se movía el agresor?" opciones={MEDIOS} iconos={ICONOS_MEDIO} valor={medio} onCambio={setMedio} error={errores.medio} />
      <Grupo
        nombre="elemento"
        titulo="¿Qué se llevaron?"
        opciones={ELEMENTOS}
        iconos={ICONOS_ELEMENTO}
        valor={elemento}
        onCambio={setElemento}
        error={errores.elementoHurtado}
      />

      <div className="campo">
        <label htmlFor="fecha">¿Cuándo?</label>
        <input
          id="fecha"
          type="datetime-local"
          value={fechaHecho}
          max={ahoraLocal()}
          onChange={(e) => setFechaHecho(e.target.value)}
          aria-invalid={Boolean(errores.fechaHecho)}
        />
        {errores.fechaHecho && <span className="error">{errores.fechaHecho}</span>}
      </div>

      <div className="campo">
        <span className="etiqueta">¿Dónde?</span>
        <span className="ayuda">Toca el mapa para mover el punto. Se usa tu ubicación actual si no lo cambias.</span>
        <MapaBase centro={punto ?? undefined} zoom={16} alto={280}>
          <Selector
            onElegir={(lat, lng) => {
              tocado.current = true
              setPunto([lat, lng])
            }}
          />
          {punto && <Marker position={punto} icon={pin} />}
        </MapaBase>
        <p className="small" aria-live="polite">
          {comuna ? `Comuna ${comuna}` : punto ? 'Fuera de las Comunas 3 y 4' : ''}
        </p>
        {errores.ubicacion && <span className="error">{errores.ubicacion}</span>}
      </div>

      {comuna && BARRIOS[comuna] && (
        <div className="campo">
          <label htmlFor="barrio">Barrio</label>
          <select id="barrio" value={barrio} onChange={(e) => setBarrio(e.target.value)}>
            <option value="">No sé o es otro barrio</option>
            {BARRIOS[comuna].map((b) => (
              <option key={b}>{b}</option>
            ))}
          </select>
        </div>
      )}

      <div className="campo">
        <label htmlFor="descripcion">Cuéntanos más (opcional)</label>
        <textarea
          id="descripcion"
          maxLength={500}
          value={descripcion}
          onChange={(e) => setDescripcion(e.target.value)}
          placeholder="Por ejemplo: dos hombres en una moto roja, uno con casco negro."
        />
        <span className="ayuda num">{descripcion.length} de 500</span>
      </div>

      {!editando && (
        <div className="campo">
          <label htmlFor="foto" className="btn secundario" style={{ justifySelf: 'start' }}>
            <Camera size={22} aria-hidden /> {foto ? 'Cambiar foto' : 'Adjuntar foto (opcional)'}
          </label>
          <input
            id="foto"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            capture="environment"
            className="sr-only"
            onChange={(e) => setFoto(e.target.files?.[0] ?? null)}
          />
          {foto && <span className="ayuda">{foto.name}</span>}
          {errores.foto && <span className="error">{errores.foto}</span>}
        </div>
      )}

      <label className="check">
        <input type="checkbox" checked={anonimo} onChange={(e) => setAnonimo(e.target.checked)} />
        <span>
          <strong>Reporte anónimo.</strong> Los vecinos no verán tu nombre.
        </span>
      </label>

      {error && (
        <p className="alerta-error" role="alert">
          {error}
        </p>
      )}
      <button className="btn bloque" disabled={enviando}>
        {enviando ? 'Enviando…' : editando ? 'Guardar cambios' : 'Enviar reporte'}
      </button>
    </form>
  )
}
