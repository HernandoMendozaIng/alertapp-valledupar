import { useEffect, useState } from 'react'
import { Printer } from '@phosphor-icons/react'
import { api, consulta } from '../api'
import { NIVELES, TIPOS } from '../catalogos'
import { Barras, Horizontales } from '../componentes/Graficas'
import type { Metricas, RespuestaHotspots } from '../tipos'

const formato = new Intl.DateTimeFormat('es-CO', { dateStyle: 'long', timeZone: 'America/Bogota' })

/**
 * HU 09 - Informe analítico semanal para la Policía y la Alcaldía. Se genera en pantalla
 * y se guarda en PDF con la impresión del navegador (Guardar como PDF).
 */
export default function Informe() {
  const [m, setM] = useState<Metricas | null>(null)
  const [zonas, setZonas] = useState<RespuestaHotspots | null>(null)

  useEffect(() => {
    api.get<Metricas>(`/analitica/metricas${consulta({ dias: 7 })}`).then(setM).catch(() => {})
    api.get<RespuestaHotspots>(`/analitica/hotspots${consulta({ dias: 30 })}`).then(setZonas).catch(() => {})
  }, [])

  const hoy = new Date()
  const inicio = new Date(Date.now() - 7 * 86400000)

  return (
    <div className="pagina ancha">
      <div className="fila entre">
        <div className="encabezado">
          <h1>Informe semanal de seguridad</h1>
          <p className="muted">
            Comunas 3 y 4 de Valledupar · del {formato.format(inicio)} al {formato.format(hoy)}
          </p>
        </div>
        <button className="btn no-imprimir" onClick={() => window.print()} disabled={!m}>
          <Printer size={20} weight="bold" aria-hidden /> Guardar como PDF
        </button>
      </div>

      {!m ? (
        <div className="esqueleto" style={{ height: 400 }} aria-busy="true" />
      ) : (
        <>
          <section className="kpis">
            <div className="kpi">
              <strong>{m.total}</strong>
              <span>reportes en la semana</span>
            </div>
            <div className="kpi">
              <strong>{m.semanaAnterior}</strong>
              <span>la semana anterior</span>
            </div>
            <div className="kpi">
              <strong>{m.validados}</strong>
              <span>validados por la comunidad</span>
            </div>
            <div className="kpi">
              <strong>{zonas?.hotspots.filter((z) => z.nivel === 'ALTO').length ?? '…'}</strong>
              <span>zonas de riesgo alto (30 días)</span>
            </div>
          </section>

          <section className="bloque">
            <h2>Recomendación de patrullaje</h2>
            {zonas && zonas.hotspots.length > 0 ? (
              <ol style={{ margin: 0, paddingLeft: 20, display: 'grid', gap: 8 }}>
                {zonas.hotspots.slice(0, 5).map((z) => (
                  <li key={z.id}>
                    Zona en {z.lat.toFixed(4)}, {z.lng.toFixed(4)} (radio {Math.round(z.radioMetros)} m): riesgo{' '}
                    <strong>{NIVELES[z.nivel].toLowerCase()}</strong>, {z.reportes} reportes, reforzar presencia entre{' '}
                    <strong className="num">{z.franjaPico}</strong>.
                  </li>
                ))}
              </ol>
            ) : (
              <p className="muted">No se identificaron zonas con suficientes reportes.</p>
            )}
          </section>

          <div className="separado" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: 16 }}>
            <section className="bloque">
              <h2>Reportes por hora</h2>
              <Barras datos={m.porHora} titulo="Reportes por hora en la semana" etiquetaCada={4} />
            </section>
            <section className="bloque">
              <h2>Tipo de hecho</h2>
              <Horizontales datos={m.porTipo} titulo="Reportes por tipo en la semana" nombres={TIPOS} />
            </section>
          </div>

          <p className="small muted">
            Fuente: reportes ciudadanos registrados en AlertApp (prototipo TRL 5, datos de prueba sintéticos). Zonas
            calientes calculadas con DBSCAN sobre los últimos 30 días.
          </p>
        </>
      )}
    </div>
  )
}
