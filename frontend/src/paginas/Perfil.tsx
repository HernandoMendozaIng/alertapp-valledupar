import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { SignOut } from '@phosphor-icons/react'
import { api, ErrorApi } from '../api'
import { useAviso } from '../aviso'
import { useSesion } from '../sesion'
import { ESTADOS, fecha, hace, TIPOS } from '../catalogos'
import type { Alerta, Reporte, Usuario } from '../tipos'

export default function Perfil() {
  const { usuario, actualizar, salir } = useSesion()
  const aviso = useAviso()
  const [mios, setMios] = useState<Reporte[] | null>(null)
  const [alertas, setAlertas] = useState<Alerta[]>([])
  const [datos, setDatos] = useState({
    nombre: usuario?.nombre ?? '',
    telefono: usuario?.telefono ?? '',
    comuna: String(usuario?.comuna ?? ''),
  })
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [guardando, setGuardando] = useState(false)

  useEffect(() => {
    api.get<Reporte[]>('/reportes/mios').then(setMios).catch(() => setMios([]))
    api.get<Alerta[]>('/alertas/mias').then(setAlertas).catch(() => {})
  }, [])

  async function guardar(e: FormEvent) {
    e.preventDefault()
    setGuardando(true)
    setErrores({})
    try {
      const u = await api.put<Usuario>('/auth/yo', {
        nombre: datos.nombre,
        telefono: datos.telefono,
        comuna: datos.comuna ? Number(datos.comuna) : null,
      })
      actualizar(u)
      aviso('Datos actualizados')
    } catch (err) {
      if (err instanceof ErrorApi) {
        setErrores(err.campos)
        aviso(err.message, 'error')
      }
    } finally {
      setGuardando(false)
    }
  }

  return (
    <div className="pagina">
      <div className="fila entre">
        <div className="encabezado">
          <h1>{usuario?.nombre}</h1>
          <p className="muted">
            {usuario?.correo} · {usuario?.rol === 'AUTORIDAD' ? 'Autoridad' : 'Ciudadano'}
          </p>
        </div>
        <button className="btn secundario chico" onClick={salir}>
          <SignOut size={18} weight="bold" aria-hidden /> Salir
        </button>
      </div>

      {usuario?.rol === 'CIUDADANO' && (
        <section className="separado" style={{ gap: 12 }}>
          <h2>Mis reportes</h2>
          {mios === null && <div className="esqueleto" style={{ height: 120 }} aria-busy="true" />}
          {mios?.length === 0 && (
            <div className="vacio">
              <p>Todavía no has reportado nada.</p>
              <Link className="btn" to="/reportar">
                Hacer mi primer reporte
              </Link>
            </div>
          )}
          {mios && mios.length > 0 && (
            <ul className="lista">
              {mios.map((r) => (
                <li key={r.id} className="item-reporte">
                  <div className="fila entre">
                    <Link className="titulo" to={`/reportes/${r.id}`}>
                      {TIPOS[r.tipoDelito]}
                    </Link>
                    <span className={`estado ${r.estado}`}>{ESTADOS[r.estado]}</span>
                  </div>
                  <div className="meta">
                    <span>{fecha(r.fechaHecho)}</span>
                    <span className="num">
                      {r.confirmaciones} confirman · {r.falsos} dicen falso
                    </span>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      {alertas.length > 0 && (
        <section className="separado" style={{ gap: 12 }}>
          <h2>Mis alertas de pánico</h2>
          <ul className="lista">
            {alertas.map((a) => (
              <li key={a.id} className="item-reporte">
                <div className="fila entre">
                  <span>{hace(a.creadoEn)}</span>
                  <span className={`estado ${a.estado}`}>{a.estado.toLowerCase()}</span>
                </div>
                {a.nota && <p className="small muted">{a.nota}</p>}
              </li>
            ))}
          </ul>
        </section>
      )}

      <form className="bloque" onSubmit={guardar} noValidate>
        <h2>Mis datos</h2>
        <div className="campo">
          <label htmlFor="p-nombre">Nombre</label>
          <input
            id="p-nombre"
            value={datos.nombre}
            onChange={(e) => setDatos({ ...datos, nombre: e.target.value })}
            aria-invalid={Boolean(errores.nombre)}
          />
          {errores.nombre && <span className="error">{errores.nombre}</span>}
        </div>
        <div className="campo">
          <label htmlFor="p-tel">Celular</label>
          <input
            id="p-tel"
            type="tel"
            inputMode="numeric"
            maxLength={10}
            value={datos.telefono}
            onChange={(e) => setDatos({ ...datos, telefono: e.target.value })}
            aria-invalid={Boolean(errores.telefono)}
          />
          {errores.telefono && <span className="error">{errores.telefono}</span>}
        </div>
        <div className="campo">
          <label htmlFor="p-comuna">Comuna</label>
          <select id="p-comuna" value={datos.comuna} onChange={(e) => setDatos({ ...datos, comuna: e.target.value })}>
            <option value="">Sin definir</option>
            {[1, 2, 3, 4, 5, 6].map((c) => (
              <option key={c} value={c}>
                Comuna {c}
              </option>
            ))}
          </select>
        </div>
        <button className="btn" style={{ justifySelf: 'start' }} disabled={guardando}>
          {guardando ? 'Guardando…' : 'Guardar'}
        </button>
      </form>
    </div>
  )
}
