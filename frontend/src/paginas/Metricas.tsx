import { useEffect, useState } from 'react'
import { api, consulta } from '../api'
import { NIVELES, TIPOS } from '../catalogos'
import { Barras, Horizontales } from '../componentes/Graficas'
import type { Metricas as TMetricas, RespuestaHotspots } from '../tipos'

/** HU 08 - Métricas y filtros dinámicos, con la analítica de puntos calientes de la IA (HU 09). */
export default function Metricas() {
  const [comuna, setComuna] = useState('')
  const [dias, setDias] = useState(90)
  const [m, setM] = useState<TMetricas | null>(null)
  const [zonas, setZonas] = useState<RespuestaHotspots | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    setError('')
    const q = consulta({ comuna, dias })
    api.get<TMetricas>(`/analitica/metricas${q}`).then(setM).catch((e) => setError(e.message))
    api.get<RespuestaHotspots>(`/analitica/hotspots${q}`).then(setZonas).catch(() => setZonas(null))
  }, [comuna, dias])

  const cambio = m && m.semanaAnterior > 0 ? Math.round(((m.ultimos7Dias - m.semanaAnterior) / m.semanaAnterior) * 100) : null
  const horaPico = m?.porHora.reduce((a, b) => (b.valor > a.valor ? b : a), m.porHora[0])
  const diaPico = m?.porDiaSemana.reduce((a, b) => (b.valor > a.valor ? b : a), m.porDiaSemana[0])

  return (
    <div className="pagina ancha">
      <div className="encabezado">
        <h1>Métricas de seguridad</h1>
        <p className="muted">Reportes no descartados de las Comunas 3 y 4. Los filtros se aplican sin recargar.</p>
      </div>

      <div className="fila">
        <div className="campo" style={{ flex: '0 1 200px' }}>
          <label htmlFor="m-comuna">Comuna</label>
          <select id="m-comuna" value={comuna} onChange={(e) => setComuna(e.target.value)}>
            <option value="">Comunas 3 y 4</option>
            <option value="3">Comuna 3</option>
            <option value="4">Comuna 4</option>
          </select>
        </div>
        <div className="campo" style={{ flex: '0 1 200px' }}>
          <label htmlFor="m-dias">Periodo</label>
          <select id="m-dias" value={dias} onChange={(e) => setDias(Number(e.target.value))}>
            <option value={7}>Últimos 7 días</option>
            <option value={30}>Últimos 30 días</option>
            <option value={90}>Últimos 90 días</option>
          </select>
        </div>
      </div>

      {error && (
        <p className="alerta-error" role="alert">
          {error}
        </p>
      )}

      {!m && !error && <div className="esqueleto" style={{ height: 420 }} aria-busy="true" />}

      {m && (
        <>
          <section className="kpis" aria-label="Resumen">
            <div className="kpi">
              <strong>{m.total}</strong>
              <span>reportes en {dias} días</span>
            </div>
            <div className="kpi">
              <strong>{m.ultimos7Dias}</strong>
              <span>
                esta semana
                {cambio !== null && ` · ${cambio > 0 ? '+' : ''}${cambio} % frente a la anterior`}
              </span>
            </div>
            <div className="kpi">
              <strong>{m.validados}</strong>
              <span>validados por vecinos</span>
            </div>
            <div className="kpi">
              <strong>{m.descartados}</strong>
              <span>anulados como falsos</span>
            </div>
          </section>

          <section className="bloque">
            <div>
              <h2>¿A qué hora pasan?</h2>
              {horaPico && horaPico.valor > 0 && (
                <p className="muted">
                  La hora con más reportes es las {horaPico.clave}:00 ({horaPico.valor} reportes).
                </p>
              )}
            </div>
            <Barras datos={m.porHora} titulo="Reportes por hora del día" etiquetaCada={3} />
          </section>

          <div className="separado" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: 16 }}>
            <section className="bloque">
              <div>
                <h2>¿Qué día?</h2>
                {diaPico && diaPico.valor > 0 && <p className="muted">El {diaPico.clave} concentra más reportes.</p>}
              </div>
              <Barras datos={m.porDiaSemana} titulo="Reportes por día de la semana" />
            </section>
            <section className="bloque">
              <h2>¿Qué pasa más?</h2>
              <Horizontales datos={m.porTipo} titulo="Reportes por tipo de hecho" nombres={TIPOS} />
              {!comuna && (
                <>
                  <h3>Por comuna</h3>
                  <Horizontales datos={m.porComuna} titulo="Reportes por comuna" />
                </>
              )}
            </section>
          </div>

          <section className="bloque">
            <div>
              <h2>Puntos calientes detectados</h2>
              <p className="muted small">
                Agrupados con DBSCAN: zonas de 200 m con al menos 4 reportes.
                {zonas && ` Motor: ${zonas.motor === 'python-sklearn-dbscan' ? 'servicio de IA en Python' : 'respaldo en Java'}.`}
              </p>
            </div>
            {zonas && zonas.hotspots.length === 0 && <p className="muted">No hay suficientes reportes para formar zonas.</p>}
            {zonas && zonas.hotspots.length > 0 && (
              <div className="tabla-envoltura" style={{ background: 'transparent' }}>
                <table>
                  <thead>
                    <tr>
                      <th>Zona</th>
                      <th>Riesgo</th>
                      <th>Reportes</th>
                      <th>Franja pico</th>
                      <th>Severidad media</th>
                    </tr>
                  </thead>
                  <tbody>
                    {zonas.hotspots.map((z, i) => (
                      <tr key={z.id}>
                        <td className="num">Zona {i + 1}</td>
                        <td>
                          <span className={`riesgo ${z.nivel}`}>{NIVELES[z.nivel]}</span>
                        </td>
                        <td className="num">{z.reportes}</td>
                        <td className="num">{z.franjaPico}</td>
                        <td className="num">{z.severidadMedia} / 100</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </>
      )}
    </div>
  )
}
