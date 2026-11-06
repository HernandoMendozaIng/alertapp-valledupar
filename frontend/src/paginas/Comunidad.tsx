import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, consulta } from '../api'
import { distancia, TIPOS } from '../catalogos'
import { useUbicacion } from '../useUbicacion'
import ItemReporte from '../componentes/ItemReporte'
import type { Reporte, TipoDelito } from '../tipos'

/** HU 07 y HU 13 - Incidentes de la zona para que los vecinos los validen y comenten. */
export default function Comunidad() {
  const ubicacion = useUbicacion()
  const [radio, setRadio] = useState<number | ''>(3000)
  const [tipo, setTipo] = useState<TipoDelito | ''>('')
  const [reportes, setReportes] = useState<Reporte[] | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    setReportes(null)
    setError('')
    const desde = new Date(Date.now() - 30 * 86400000).toISOString()
    api
      .get<Reporte[]>(
        `/reportes${consulta({
          desde,
          tipo,
          estado: 'PENDIENTE,VALIDADO,EN_REVISION',
          lat: radio ? ubicacion.lat : undefined,
          lng: radio ? ubicacion.lng : undefined,
          radio: radio || undefined,
        })}`,
      )
      .then(setReportes)
      .catch((e) => setError(e.message))
  }, [radio, tipo, ubicacion.lat, ubicacion.lng])

  const conDistancia = useMemo(
    () =>
      (reportes ?? []).map((r) => ({
        r,
        d: distancia(ubicacion.lat, ubicacion.lng, r.latitud, r.longitud),
      })),
    [reportes, ubicacion.lat, ubicacion.lng],
  )

  function reemplazar(nuevo: Reporte) {
    setReportes((l) => l?.map((x) => (x.id === nuevo.id ? nuevo : x)) ?? null)
  }

  return (
    <div className="pagina">
      <div className="encabezado">
        <h1>Comunidad</h1>
        <p className="muted">
          Confirma lo que viste o marca como falso lo que no pasó. Con tres votos de diferencia el reporte se valida o se
          anula solo.
        </p>
      </div>

      <div className="fila">
        <div className="campo" style={{ flex: '1 1 160px' }}>
          <label htmlFor="radio">Distancia</label>
          <select id="radio" value={radio} onChange={(e) => setRadio(e.target.value ? Number(e.target.value) : '')}>
            <option value={500}>A 500 m</option>
            <option value={1000}>A 1 km</option>
            <option value={3000}>A 3 km</option>
            <option value="">Todas las Comunas 3 y 4</option>
          </select>
        </div>
        <div className="campo" style={{ flex: '1 1 160px' }}>
          <label htmlFor="tipo">Tipo</label>
          <select id="tipo" value={tipo} onChange={(e) => setTipo(e.target.value as TipoDelito | '')}>
            <option value="">Todos</option>
            {Object.entries(TIPOS).map(([k, v]) => (
              <option key={k} value={k}>
                {v}
              </option>
            ))}
          </select>
        </div>
      </div>

      {error && (
        <p className="alerta-error" role="alert">
          {error}
        </p>
      )}

      {reportes === null && !error && (
        <div className="lista" aria-busy="true">
          {[0, 1, 2].map((i) => (
            <div key={i} className="esqueleto" style={{ height: 150 }} />
          ))}
        </div>
      )}

      {reportes?.length === 0 && (
        <div className="vacio">
          <h2>No hay reportes en esta zona</h2>
          <p>En los últimos 30 días nadie ha reportado nada aquí. Amplía la distancia o reporta lo que veas.</p>
          <Link className="btn" to="/reportar">
            Reportar un hecho
          </Link>
        </div>
      )}

      {reportes && reportes.length > 0 && (
        <>
          <p className="small muted num">{reportes.length} reportes en los últimos 30 días</p>
          <ul className="lista">
            {conDistancia.map(({ r, d }) => (
              <ItemReporte key={r.id} reporte={r} distancia={ubicacion.real ? d : undefined} alCambiar={reemplazar} />
            ))}
          </ul>
        </>
      )}
    </div>
  )
}
