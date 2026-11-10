import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { CircleMarker, Popup } from 'react-leaflet'
import { Phone, ShieldCheck, Siren, Trash, UserPlus } from '@phosphor-icons/react'
import { api, ErrorApi } from '../api'
import { useAviso } from '../aviso'
import { hace } from '../catalogos'
import { useEvento } from '../tiempoReal'
import { restante } from '../seguimiento'
import MapaBase from '../componentes/MapaBase'
import type { Guardian, Notificacion, Protegido } from '../tipos'

const MAXIMO = 3

/**
 * HU 12 - Red de apoyo. Arriba, las personas que cuido (con su ubicación en vivo si están en alerta o en trayecto).
 * Abajo, mis guardianes: hasta 3 personas registradas que reciben mis alertas.
 */
export default function Red() {
  const aviso = useAviso()
  const [guardianes, setGuardianes] = useState<Guardian[] | null>(null)
  const [protegidos, setProtegidos] = useState<Protegido[] | null>(null)
  const [correo, setCorreo] = useState('')
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)

  const cargarProtegidos = useCallback(() => {
    api.get<Protegido[]>('/guardianes/protegidos').then(setProtegidos).catch(() => setProtegidos([]))
  }, [])

  useEffect(() => {
    api.get<Guardian[]>('/guardianes').then(setGuardianes).catch(() => setGuardianes([]))
    cargarProtegidos()
    // Respaldo por si el canal en tiempo real se cae.
    const t = window.setInterval(cargarProtegidos, 20000)
    return () => window.clearInterval(t)
  }, [cargarProtegidos])

  // La posición del protegido llega por el canal en tiempo real y mueve el punto sin recargar.
  useEvento('ubicacion', (d) => {
    const u = d as { usuarioId: number; latitud: number; longitud: number; ubicacionEn: string }
    setProtegidos((lista) =>
      lista?.map((p) =>
        p.usuarioId === u.usuarioId ? { ...p, latitud: u.latitud, longitud: u.longitud, ubicacionEn: u.ubicacionEn } : p,
      ) ?? null,
    )
  })
  useEvento('protegido', () => cargarProtegidos())
  useEvento('notificacion', (d) => {
    const n = d as Notificacion
    if (n.tipo === 'PANICO_PROTEGIDO' || n.tipo === 'SEGUIMIENTO_PROTEGIDO' || n.tipo === 'GUARDIAN_AGREGADO') cargarProtegidos()
  })

  async function agregar(e: FormEvent) {
    e.preventDefault()
    setError('')
    setEnviando(true)
    try {
      const g = await api.post<Guardian>('/guardianes', { correo })
      setGuardianes((l) => [...(l ?? []), g])
      setCorreo('')
      aviso(`${g.nombre.split(' ')[0]} ahora es tu guardián`)
    } catch (err) {
      setError(err instanceof ErrorApi ? err.message : 'No se pudo agregar')
    } finally {
      setEnviando(false)
    }
  }

  async function quitar(g: Guardian) {
    try {
      await api.delete(`/guardianes/${g.id}`)
      setGuardianes((l) => l?.filter((x) => x.id !== g.id) ?? null)
      aviso(`Quitaste a ${g.nombre.split(' ')[0]} de tu red`)
    } catch (err) {
      aviso((err as Error).message, 'error')
    }
  }

  const enAlerta = protegidos?.filter((p) => p.latitud !== null) ?? []
  const lleno = (guardianes?.length ?? 0) >= MAXIMO

  return (
    <div className="pagina">
      <div className="encabezado">
        <h1>Mi red de apoyo</h1>
        <p className="muted">
          Tus guardianes reciben un aviso al instante si activas el botón de pánico o compartes un trayecto, y ven dónde
          estás solo mientras dure.
        </p>
      </div>

      {protegidos && protegidos.length > 0 && (
        <section className="separado" style={{ gap: 12 }} aria-labelledby="titulo-cuido">
          <h2 id="titulo-cuido">Personas que cuido</h2>
          {enAlerta.length > 0 && (
            <MapaBase centro={[enAlerta[0].latitud!, enAlerta[0].longitud!]} zoom={16} alto={260}>
              {enAlerta.map((p) => (
                <CircleMarker
                  key={p.usuarioId}
                  center={[p.latitud!, p.longitud!]}
                  radius={10}
                  pathOptions={{
                    color: '#ffffff',
                    weight: 3,
                    fillColor: p.estado === 'PANICO' ? '#d32f2f' : '#1d6fd1',
                    fillOpacity: 1,
                  }}
                >
                  <Popup>
                    <strong>{p.nombre}</strong>
                    <br />
                    {p.ubicacionEn ? `Actualizado ${hace(p.ubicacionEn)}` : ''}
                  </Popup>
                </CircleMarker>
              ))}
            </MapaBase>
          )}
          <ul className="lista">
            {protegidos.map((p) => (
              <li key={p.usuarioId} className={`item-reporte protegido ${p.estado}`}>
                <div className="fila entre">
                  <strong>{p.nombre}</strong>
                  <span className={`estado ${p.estado === 'PANICO' ? 'ACTIVA' : p.estado === 'TRAYECTO' ? 'EN_REVISION' : 'VALIDADO'}`}>
                    {p.estado === 'PANICO' ? 'Botón de pánico' : p.estado === 'TRAYECTO' ? 'En trayecto' : 'Sin alertas'}
                  </span>
                </div>
                {p.estado === 'PANICO' && (
                  <p className="small">
                    <Siren size={16} weight="fill" aria-hidden style={{ verticalAlign: '-3px', color: 'var(--riesgo-alto)' }} />{' '}
                    Pidió ayuda. Llámalo y, si no contesta, llama al 123.
                  </p>
                )}
                {p.estado !== 'TRANQUILO' && p.ubicacionEn && (
                  <p className="small muted">
                    Ubicación de {hace(p.ubicacionEn)}
                    {p.expiraEn ? ` · ${restante(p.expiraEn)}` : ''}
                  </p>
                )}
                {p.telefono && p.estado !== 'TRANQUILO' && (
                  <div className="fila">
                    <a className={`btn chico ${p.estado === 'PANICO' ? 'peligro' : ''}`} href={`tel:${p.telefono}`}>
                      <Phone size={16} weight="bold" aria-hidden /> Llamar
                    </a>
                    {p.estado === 'PANICO' && (
                      <a className="btn chico secundario" href="tel:123">
                        Llamar al 123
                      </a>
                    )}
                  </div>
                )}
              </li>
            ))}
          </ul>
        </section>
      )}

      <section className="bloque" aria-labelledby="titulo-guardianes">
        <div className="fila entre">
          <h2 id="titulo-guardianes">Mis guardianes</h2>
          <span className="muted num small">
            {guardianes?.length ?? 0} de {MAXIMO}
          </span>
        </div>
        {guardianes === null && <div className="esqueleto" style={{ height: 80 }} aria-busy="true" />}
        {guardianes?.length === 0 && (
          <div className="vacio" style={{ padding: 16 }}>
            <ShieldCheck size={32} weight="duotone" aria-hidden />
            <p>Todavía no tienes guardianes. Agrega a un familiar o a un vecino de confianza que use AlertApp.</p>
          </div>
        )}
        {guardianes && guardianes.length > 0 && (
          <ul className="lista">
            {guardianes.map((g) => (
              <li key={g.id} className="fila entre item-reporte">
                <div>
                  <strong>{g.nombre}</strong>
                  <p className="small muted">{g.correo}</p>
                </div>
                <button className="btn texto chico" onClick={() => quitar(g)} aria-label={`Quitar a ${g.nombre}`}>
                  <Trash size={16} weight="bold" aria-hidden /> Quitar
                </button>
              </li>
            ))}
          </ul>
        )}
        <form onSubmit={agregar} noValidate style={{ display: 'grid', gap: 12 }}>
          <div className="campo">
            <label htmlFor="g-correo">Correo de la persona</label>
            <input
              id="g-correo"
              type="email"
              inputMode="email"
              autoComplete="off"
              value={correo}
              onChange={(e) => setCorreo(e.target.value)}
              disabled={lleno}
              aria-invalid={Boolean(error)}
              aria-describedby="g-ayuda"
            />
            <span id="g-ayuda" className="ayuda">
              {lleno ? 'Ya tienes 3 guardianes. Quita uno para agregar otro.' : 'Debe tener una cuenta en AlertApp.'}
            </span>
            {error && <span className="error">{error}</span>}
          </div>
          <button className="btn" style={{ justifySelf: 'start' }} disabled={lleno || enviando || !correo}>
            <UserPlus size={18} weight="bold" aria-hidden /> {enviando ? 'Agregando…' : 'Agregar guardián'}
          </button>
        </form>
      </section>
    </div>
  )
}
