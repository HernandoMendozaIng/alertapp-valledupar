import { useNavigate } from 'react-router-dom'
import { BellSimple, MapPin, ShieldCheck, Siren, UserPlus, ClipboardText } from '@phosphor-icons/react'
import { hace } from '../catalogos'
import { useTiempoReal } from '../tiempoReal'
import type { TipoNotificacion } from '../tipos'

const ICONO: Record<TipoNotificacion, typeof Siren> = {
  REPORTE_CERCANO: MapPin,
  PANICO_PROTEGIDO: Siren,
  SEGUIMIENTO_PROTEGIDO: ShieldCheck,
  GUARDIAN_AGREGADO: UserPlus,
  ESTADO_REPORTE: ClipboardText,
}

/** HU 02 - Bandeja de alertas. Las nuevas llegan solas por el canal en tiempo real. */
export default function Notificaciones() {
  const { bandeja, marcarLeida, marcarTodas, conectado } = useTiempoReal()
  const navegar = useNavigate()

  return (
    <div className="pagina">
      <div className="fila entre">
        <div className="encabezado">
          <h1>Avisos</h1>
          <p className="small muted">
            <span className={`punto-conexion ${conectado ? 'si' : ''}`} aria-hidden />
            {conectado ? 'Recibiendo alertas en tiempo real' : 'Reconectando…'}
          </p>
        </div>
        {bandeja.sinLeer > 0 && (
          <button className="btn secundario chico" onClick={marcarTodas}>
            Marcar todo como leído
          </button>
        )}
      </div>

      {bandeja.notificaciones.length === 0 ? (
        <div className="vacio">
          <BellSimple size={36} weight="duotone" aria-hidden />
          <p>
            No tienes avisos. Te escribiremos aquí si reportan algo cerca de ti, si alguien que cuidas pide ayuda o si
            la autoridad responde a tus reportes.
          </p>
        </div>
      ) : (
        <ul className="lista">
          {bandeja.notificaciones.map((n) => {
            const Icono = ICONO[n.tipo]
            return (
              <li key={n.id}>
                <button
                  className={`aviso-item ${n.leida ? '' : 'nuevo'} ${n.tipo}`}
                  onClick={() => {
                    if (!n.leida) marcarLeida(n.id)
                    if (n.enlace) navegar(n.enlace)
                  }}
                >
                  <Icono size={22} weight={n.tipo === 'PANICO_PROTEGIDO' ? 'fill' : 'bold'} aria-hidden />
                  <span>
                    <strong>{n.titulo}</strong>
                    <span className="small">{n.cuerpo}</span>
                    <span className="small muted">{hace(n.creadoEn)}</span>
                  </span>
                  {!n.leida && <span className="sr-only">Sin leer</span>}
                </button>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
