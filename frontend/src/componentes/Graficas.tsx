import type { Conteo } from '../tipos'

/** Barras verticales de una sola serie. El valor máximo se rotula directamente sobre su barra. */
export function Barras({
  datos,
  titulo,
  unidad = 'reportes',
  etiquetaCada = 1,
}: {
  datos: Conteo[]
  titulo: string
  unidad?: string
  etiquetaCada?: number
}) {
  const max = Math.max(1, ...datos.map((d) => d.valor))
  const pico = datos.findIndex((d) => d.valor === max)
  const columnas = { gridTemplateColumns: `repeat(${datos.length}, 1fr)` }
  return (
    <figure style={{ margin: 0 }} className="separado">
      <figcaption className="sr-only">{titulo}</figcaption>
      <div style={{ display: 'grid', gap: 6 }}>
        <div className="barras" style={columnas} aria-hidden>
          {datos.map((d, i) => (
            <div
              key={d.clave}
              className={`barra ${i === pico && max > 0 ? 'pico' : ''}`}
              tabIndex={-1}
              style={{ paddingTop: 22 }}
            >
              {i === pico && (
                <span className="small num" style={{ position: 'absolute', top: 0, left: '50%', transform: 'translateX(-50%)', fontWeight: 700 }}>
                  {d.valor}
                </span>
              )}
              <span className="consejo num">
                {d.clave}: {d.valor} {unidad}
              </span>
              <div style={{ height: `${(d.valor / max) * 100}%`, animationDelay: `${i * 18}ms` }} />
            </div>
          ))}
        </div>
        <div className="ejes" style={columnas} aria-hidden>
          {datos.map((d, i) => (
            <span key={d.clave}>{i % etiquetaCada === 0 ? d.clave : ''}</span>
          ))}
        </div>
      </div>
      <TablaAccesible datos={datos} titulo={titulo} />
    </figure>
  )
}

/** Barras horizontales ordenadas, para categorías con nombre largo. */
export function Horizontales({ datos, titulo, nombres }: { datos: Conteo[]; titulo: string; nombres?: Record<string, string> }) {
  const max = Math.max(1, ...datos.map((d) => d.valor))
  return (
    <figure style={{ margin: 0 }}>
      <figcaption className="sr-only">{titulo}</figcaption>
      <div className="horizontales" aria-hidden>
        {datos.map((d, i) => (
          <div key={d.clave} className="horizontal">
            <span>{nombres?.[d.clave] ?? d.clave}</span>
            <div className="pista">
              <div style={{ width: `${(d.valor / max) * 100}%`, animationDelay: `${i * 40}ms` }} />
            </div>
            <span className="num" style={{ fontWeight: 600 }}>
              {d.valor}
            </span>
          </div>
        ))}
      </div>
      <TablaAccesible datos={datos} titulo={titulo} nombres={nombres} />
    </figure>
  )
}

function TablaAccesible({ datos, titulo, nombres }: { datos: Conteo[]; titulo: string; nombres?: Record<string, string> }) {
  return (
    <table className="sr-only">
      <caption>{titulo}</caption>
      <tbody>
        {datos.map((d) => (
          <tr key={d.clave}>
            <th scope="row">{nombres?.[d.clave] ?? d.clave}</th>
            <td>{d.valor}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
