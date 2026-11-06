import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { DownloadSimple, FileText, MapPin } from '@phosphor-icons/react'
import { api, consulta, descargar } from '../api'
import { useAviso } from '../aviso'
import { ESTADOS, fecha, hace, NIVELES, TIPOS } from '../catalogos'
import type { Alerta, EstadoReporte, Reporte, Severidad } from '../tipos'

const SIGUIENTES: Partial<Record<EstadoReporte, EstadoReporte[]>> = {
  PENDIENTE: ['EN_REVISION', 'DESCARTADO'],
  VALIDADO: ['EN_REVISION', 'DESCARTADO'],
  EN_REVISION: ['CERRADO', 'DESCARTADO'],
  DESCARTADO: ['EN_REVISION'],
  CERRADO: ['EN_REVISION'],
}

/** Panel web de la autoridad: alertas de pánico (HU 11), gestión de casos (HU 13) y exportación (HU 09). */
export default function Panel() {
  const aviso = useAviso()
  const [alertas, setAlertas] = useState<Alerta[]>([])
  const [reportes, setReportes] = useState<Reporte[] | null>(null)
  const [estado, setEstado] = useState<EstadoReporte | ''>('VALIDADO')
  const [comuna, setComuna] = useState('')
  const [severidad, setSeveridad] = useState<Severidad | ''>('')
  const [exportando, setExportando] = useState(false)

  const cargarAlertas = useCallback(() => {
    api.get<Alerta[]>('/panel/alertas').then(setAlertas).catch(() => {})
  }, [])

  useEffect(() => {
    cargarAlertas()
    const id = window.setInterval(cargarAlertas, 15000)
    return () => window.clearInterval(id)
  }, [cargarAlertas])

  useEffect(() => {
    setReportes(null)
    api
      .get<Reporte[]>(`/reportes${consulta({ estado, comuna, severidad })}`)
      .then(setReportes)
      .catch((e) => aviso(e.message, 'error'))
  }, [estado, comuna, severidad, aviso])

  async function atender(a: Alerta) {
    try {
      await api.post(`/panel/alertas/${a.id}/atender`, { nota: 'Patrulla enviada al sitio' })
      aviso('Alerta marcada como atendida')
      cargarAlertas()
    } catch (e) {
      aviso((e as Error).message, 'error')
    }
  }

  async function cambiar(r: Reporte, nuevo: EstadoReporte) {
    try {
      const act = await api.patch<Reporte>(`/panel/reportes/${r.id}/estado`, { estado: nuevo })
      setReportes((l) => (estado && act.estado !== estado ? l?.filter((x) => x.id !== r.id) : l?.map((x) => (x.id === r.id ? act : x))) ?? null)
      aviso(`Reporte #${r.id}: ${ESTADOS[act.estado].toLowerCase()}`)
    } catch (e) {
      aviso((e as Error).message, 'error')
    }
  }

  async function exportar() {
    setExportando(true)
    try {
      const { blob, nombre } = await descargar(`/analitica/exportar${consulta({ comuna, dias: 7 })}`)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = nombre ?? 'alertapp-reportes.csv'
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      aviso((e as Error).message, 'error')
    } finally {
      setExportando(false)
    }
  }

  const activas = alertas.filter((a) => a.estado === 'ACTIVA')

  return (
    <div className="pagina ancha">
      <div className="fila entre">
        <div className="encabezado">
          <h1>Panel de autoridad</h1>
          <p className="muted">Policía Nacional y Alcaldía de Valledupar · Comunas 3 y 4</p>
        </div>
        <div className="fila">
          <button className="btn secundario" onClick={exportar} disabled={exportando}>
            <DownloadSimple size={20} weight="bold" aria-hidden /> {exportando ? 'Generando…' : 'Exportar semana (CSV)'}
          </button>
          <Link className="btn" to="/panel/informe">
            <FileText size={20} weight="bold" aria-hidden /> Informe semanal
          </Link>
        </div>
      </div>

      <section className="separado" style={{ gap: 12 }} aria-live="polite">
        <h2>
          Alertas de pánico activas <span className="muted num">({activas.length})</span>
        </h2>
        {activas.length === 0 ? (
          <p className="muted">No hay alertas activas. La lista se actualiza cada 15 segundos.</p>
        ) : (
          <ul className="lista">
            {activas.map((a) => (
              <li key={a.id} className="item-reporte">
                <div className="fila entre">
                  <strong>
                    {a.usuario} {a.telefono && <a href={`tel:${a.telefono}`}>{a.telefono}</a>}
                  </strong>
                  <span className="estado ACTIVA">Activa {hace(a.creadoEn)}</span>
                </div>
                <div className="fila">
                  <a
                    className="btn secundario chico"
                    href={`https://www.openstreetmap.org/?mlat=${a.latitud}&mlon=${a.longitud}#map=18/${a.latitud}/${a.longitud}`}
                    target="_blank"
                    rel="noreferrer"
                  >
                    <MapPin size={18} aria-hidden /> Ver ubicación
                  </a>
                  <button className="btn chico" onClick={() => atender(a)}>
                    Marcar atendida
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="separado" style={{ gap: 12 }}>
        <h2>Reportes</h2>
        <div className="fila">
          <div className="campo" style={{ flex: '0 1 220px' }}>
            <label htmlFor="f-estado">Estado</label>
            <select id="f-estado" value={estado} onChange={(e) => setEstado(e.target.value as EstadoReporte | '')}>
              <option value="">Todos</option>
              {Object.entries(ESTADOS).map(([k, v]) => (
                <option key={k} value={k}>
                  {v}
                </option>
              ))}
            </select>
          </div>
          <div className="campo" style={{ flex: '0 1 180px' }}>
            <label htmlFor="f-comuna">Comuna</label>
            <select id="f-comuna" value={comuna} onChange={(e) => setComuna(e.target.value)}>
              <option value="">3 y 4</option>
              <option value="3">Comuna 3</option>
              <option value="4">Comuna 4</option>
            </select>
          </div>
          <div className="campo" style={{ flex: '0 1 180px' }}>
            <label htmlFor="f-sev">Severidad</label>
            <select id="f-sev" value={severidad} onChange={(e) => setSeveridad(e.target.value as Severidad | '')}>
              <option value="">Todas</option>
              <option value="ALTA">Alta</option>
              <option value="MEDIA">Media</option>
              <option value="BAJA">Baja</option>
            </select>
          </div>
        </div>

        {reportes === null ? (
          <div className="esqueleto" style={{ height: 300 }} aria-busy="true" />
        ) : reportes.length === 0 ? (
          <div className="vacio">
            <p>No hay reportes con estos filtros.</p>
          </div>
        ) : (
          <div className="tabla-envoltura">
            <table>
              <thead>
                <tr>
                  <th>#</th>
                  <th>Hecho</th>
                  <th>Fecha</th>
                  <th>Lugar</th>
                  <th>Severidad</th>
                  <th>Votos</th>
                  <th>Estado</th>
                  <th>Cambiar a</th>
                </tr>
              </thead>
              <tbody>
                {reportes.slice(0, 100).map((r) => (
                  <tr key={r.id}>
                    <td className="num">
                      <Link to={`/reportes/${r.id}`}>{r.id}</Link>
                    </td>
                    <td>{TIPOS[r.tipoDelito]}</td>
                    <td className="num" style={{ whiteSpace: 'nowrap' }}>
                      {fecha(r.fechaHecho)}
                    </td>
                    <td>{r.barrio ?? `Comuna ${r.comuna}`}</td>
                    <td>
                      <span className={`riesgo ${r.severidad}`}>{NIVELES[r.severidad]}</span>
                    </td>
                    <td className="num">
                      +{r.confirmaciones} / −{r.falsos}
                    </td>
                    <td>
                      <span className={`estado ${r.estado}`}>{ESTADOS[r.estado]}</span>
                    </td>
                    <td>
                      <label className="sr-only" htmlFor={`cambio-${r.id}`}>
                        Cambiar estado del reporte {r.id}
                      </label>
                      <select
                        id={`cambio-${r.id}`}
                        value=""
                        onChange={(e) => e.target.value && cambiar(r, e.target.value as EstadoReporte)}
                      >
                        <option value="">Elige…</option>
                        {SIGUIENTES[r.estado]?.map((s) => (
                          <option key={s} value={s}>
                            {ESTADOS[s]}
                          </option>
                        ))}
                      </select>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {reportes && reportes.length > 100 && (
          <p className="small muted">Se muestran los 100 más recientes de {reportes.length}. Usa los filtros para acotar.</p>
        )}
      </section>
    </div>
  )
}
