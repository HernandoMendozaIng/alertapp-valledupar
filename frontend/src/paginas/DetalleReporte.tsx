import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { CircleMarker } from 'react-leaflet'
import { ArrowLeft, PencilSimple, Trash } from '@phosphor-icons/react'
import { api, descargar } from '../api'
import { useAviso } from '../aviso'
import { useSesion } from '../sesion'
import { ARMAS, ELEMENTOS, ESTADOS, fecha, hace, MEDIOS, NIVELES, TIPOS } from '../catalogos'
import MapaBase, { COLOR_NIVEL } from '../componentes/MapaBase'
import { Votos } from '../componentes/ItemReporte'
import type { Comentario, EstadoReporte, Reporte } from '../tipos'

const SIGUIENTES: Partial<Record<EstadoReporte, EstadoReporte[]>> = {
  PENDIENTE: ['EN_REVISION', 'DESCARTADO'],
  VALIDADO: ['EN_REVISION', 'DESCARTADO'],
  EN_REVISION: ['CERRADO', 'DESCARTADO'],
  DESCARTADO: ['EN_REVISION'],
  CERRADO: ['EN_REVISION'],
}

export default function DetalleReporte() {
  const { id } = useParams()
  const navegar = useNavigate()
  const aviso = useAviso()
  const { usuario } = useSesion()
  const [reporte, setReporte] = useState<Reporte | null>(null)
  const [comentarios, setComentarios] = useState<Comentario[]>([])
  const [foto, setFoto] = useState<string | null>(null)
  const [texto, setTexto] = useState('')
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [nuevoEstado, setNuevoEstado] = useState<EstadoReporte | ''>('')
  const [nota, setNota] = useState('')

  useEffect(() => {
    api.get<Reporte>(`/reportes/${id}`).then(setReporte).catch((e) => setError(e.message))
    api.get<Comentario[]>(`/reportes/${id}/comentarios`).then(setComentarios).catch(() => {})
  }, [id])

  useEffect(() => {
    if (!reporte?.tieneEvidencia) return
    let url: string | null = null
    descargar(`/reportes/${reporte.id}/evidencia`)
      .then(({ blob }) => {
        url = URL.createObjectURL(blob)
        setFoto(url)
      })
      .catch(() => {})
    return () => {
      if (url) URL.revokeObjectURL(url)
    }
  }, [reporte?.id, reporte?.tieneEvidencia])

  async function comentar(e: FormEvent) {
    e.preventDefault()
    if (!texto.trim()) return
    setEnviando(true)
    try {
      const c = await api.post<Comentario>(`/reportes/${id}/comentarios`, { texto })
      setComentarios((l) => [...l, c])
      setTexto('')
    } catch (err) {
      aviso((err as Error).message, 'error')
    } finally {
      setEnviando(false)
    }
  }

  async function borrarComentario(c: Comentario) {
    try {
      await api.delete(`/comentarios/${c.id}`)
      setComentarios((l) => l.filter((x) => x.id !== c.id))
    } catch (err) {
      aviso((err as Error).message, 'error')
    }
  }

  async function eliminar() {
    if (!window.confirm('¿Eliminar este reporte? No se puede deshacer.')) return
    try {
      await api.delete(`/reportes/${id}`)
      aviso('Reporte eliminado')
      navegar('/perfil', { replace: true })
    } catch (err) {
      aviso((err as Error).message, 'error')
    }
  }

  async function cambiarEstado(e: FormEvent) {
    e.preventDefault()
    if (!nuevoEstado) return
    try {
      const r = await api.patch<Reporte>(`/panel/reportes/${id}/estado`, { estado: nuevoEstado, nota })
      setReporte(r)
      setNuevoEstado('')
      setNota('')
      api.get<Comentario[]>(`/reportes/${id}/comentarios`).then(setComentarios)
      aviso(`Estado cambiado a ${ESTADOS[r.estado].toLowerCase()}`)
    } catch (err) {
      aviso((err as Error).message, 'error')
    }
  }

  if (error)
    return (
      <div className="pagina">
        <div className="vacio">
          <h2>No encontramos el reporte</h2>
          <p>{error}</p>
          <Link className="btn" to="/comunidad">
            Volver a la comunidad
          </Link>
        </div>
      </div>
    )

  if (!reporte)
    return (
      <div className="pagina" aria-busy="true">
        <div className="esqueleto" style={{ height: 40 }} />
        <div className="esqueleto" style={{ height: 240 }} />
      </div>
    )

  const r = reporte
  const esAutoridad = usuario?.rol === 'AUTORIDAD'

  return (
    <div className="pagina">
      <button className="btn texto" style={{ justifySelf: 'start' }} onClick={() => navegar(-1)}>
        <ArrowLeft size={18} weight="bold" aria-hidden /> Volver
      </button>

      <div className="encabezado">
        <div className="fila entre">
          <h1>{TIPOS[r.tipoDelito]}</h1>
          <span className={`riesgo ${r.severidad}`} title={`Puntaje de severidad ${r.puntajeSeveridad} de 100`}>
            Severidad {NIVELES[r.severidad].toLowerCase()}
          </span>
        </div>
        <div className="meta">
          <span>{fecha(r.fechaHecho)}</span>
          <span>
            {r.barrio ? `${r.barrio}, ` : ''}Comuna {r.comuna}
          </span>
          <span>{r.anonimo ? 'Reporte anónimo' : `Reportó ${r.autor}`}</span>
        </div>
      </div>

      <MapaBase centro={[r.latitud, r.longitud]} zoom={16} alto={220} interactivo={false}>
        <CircleMarker
          center={[r.latitud, r.longitud]}
          radius={10}
          pathOptions={{ color: '#fff', weight: 3, fillColor: COLOR_NIVEL[r.severidad], fillOpacity: 1 }}
        />
      </MapaBase>

      <section className="bloque">
        <dl className="separado" style={{ gap: 12, margin: 0 }}>
          {[
            ['Arma', ARMAS[r.arma]],
            ['Medio del agresor', MEDIOS[r.medio]],
            ['Se llevaron', ELEMENTOS[r.elementoHurtado]],
            ['Estado', ESTADOS[r.estado]],
          ].map(([k, v]) => (
            <div key={k} className="fila entre">
              <dt className="muted">{k}</dt>
              <dd style={{ margin: 0, fontWeight: 600 }}>{v}</dd>
            </div>
          ))}
        </dl>
        {r.descripcion && <p>{r.descripcion}</p>}
        {foto && <img className="foto-evidencia" src={foto} alt="Foto de evidencia adjunta al reporte" />}
      </section>

      <section className="bloque">
        <h2>Validación de la comunidad</h2>
        <Votos reporte={r} alCambiar={setReporte} />
        {r.esMio && r.estado === 'PENDIENTE' && (
          <div className="fila">
            <Link className="btn secundario chico" to={`/reportes/${r.id}/editar`}>
              <PencilSimple size={18} aria-hidden /> Editar
            </Link>
            <button className="btn texto chico" style={{ color: 'var(--alto-tinta)' }} onClick={eliminar}>
              <Trash size={18} aria-hidden /> Eliminar
            </button>
          </div>
        )}
        {r.esMio && r.estado !== 'PENDIENTE' && (
          <p className="small muted">Este reporte ya fue {ESTADOS[r.estado].toLowerCase()} y no se puede editar.</p>
        )}
      </section>

      {esAutoridad && SIGUIENTES[r.estado] && (
        <form className="bloque" onSubmit={cambiarEstado}>
          <h2>Gestión de la autoridad</h2>
          <div className="campo">
            <label htmlFor="estado">Nuevo estado</label>
            <select id="estado" value={nuevoEstado} onChange={(e) => setNuevoEstado(e.target.value as EstadoReporte)}>
              <option value="">Elige…</option>
              {SIGUIENTES[r.estado]!.map((e) => (
                <option key={e} value={e}>
                  {ESTADOS[e]}
                </option>
              ))}
            </select>
          </div>
          <div className="campo">
            <label htmlFor="nota">Nota para el ciudadano (opcional)</label>
            <input id="nota" value={nota} maxLength={400} onChange={(e) => setNota(e.target.value)} />
          </div>
          <button className="btn" disabled={!nuevoEstado}>
            Guardar estado
          </button>
        </form>
      )}

      <section className="bloque">
        <h2>
          Comentarios <span className="muted num">({comentarios.length})</span>
        </h2>
        {comentarios.length === 0 && <p className="muted">Nadie ha comentado todavía.</p>}
        <div>
          {comentarios.map((c) => (
            <div key={c.id} className={`comentario ${c.esAutoridad ? 'autoridad' : ''}`}>
              <div className="fila entre">
                <strong>{c.autor}</strong>
                <span className="small muted">{hace(c.creadoEn)}</span>
              </div>
              <p>{c.texto}</p>
              {c.esMio && (
                <button className="btn texto chico" style={{ justifySelf: 'start' }} onClick={() => borrarComentario(c)}>
                  Borrar
                </button>
              )}
            </div>
          ))}
        </div>
        <form className="separado" style={{ gap: 8 }} onSubmit={comentar}>
          <label htmlFor="comentario" className="sr-only">
            Escribe un comentario
          </label>
          <textarea
            id="comentario"
            value={texto}
            maxLength={400}
            onChange={(e) => setTexto(e.target.value)}
            placeholder="¿Viste algo? Agrega un dato que ayude."
          />
          <button className="btn" style={{ justifySelf: 'start' }} disabled={enviando || !texto.trim()}>
            Comentar
          </button>
        </form>
      </section>
    </div>
  )
}
