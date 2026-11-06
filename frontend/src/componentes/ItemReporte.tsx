import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ChatCircle, ThumbsDown, ThumbsUp } from '@phosphor-icons/react'
import { api } from '../api'
import { useAviso } from '../aviso'
import { ESTADOS, hace, metros, NIVELES, TIPOS, ARMAS, MEDIO_FRASE, ELEMENTOS } from '../catalogos'
import type { Reporte } from '../tipos'

const VOTABLE = new Set(['PENDIENTE', 'VALIDADO'])

/** Botones de validación comunitaria (HU 07). Devuelve el reporte actualizado al padre. */
export function Votos({ reporte, alCambiar }: { reporte: Reporte; alCambiar: (r: Reporte) => void }) {
  const aviso = useAviso()
  const [enviando, setEnviando] = useState(false)
  const bloqueado = reporte.esMio || !VOTABLE.has(reporte.estado) || enviando

  async function votar(tipo: 'CONFIRMA' | 'FALSO') {
    setEnviando(true)
    try {
      const r =
        reporte.miVoto === tipo
          ? await api.delete<Reporte>(`/reportes/${reporte.id}/voto`)
          : await api.put<Reporte>(`/reportes/${reporte.id}/voto`, { tipo })
      alCambiar(r)
      if (r.estado !== reporte.estado && r.estado === 'VALIDADO') aviso('Con tu voto, el reporte quedó validado')
      if (r.estado !== reporte.estado && r.estado === 'DESCARTADO') aviso('El reporte se anuló por votos de falso')
    } catch (e) {
      aviso((e as Error).message, 'error')
    } finally {
      setEnviando(false)
    }
  }

  const titulo = reporte.esMio ? 'No puedes votar tu propio reporte' : undefined
  return (
    <div className="votos" title={titulo}>
      <button
        className="voto confirma"
        aria-pressed={reporte.miVoto === 'CONFIRMA'}
        disabled={bloqueado}
        onClick={() => votar('CONFIRMA')}
      >
        <ThumbsUp size={18} weight="bold" aria-hidden /> Confirmo <span className="num">{reporte.confirmaciones}</span>
      </button>
      <button
        className="voto falso"
        aria-pressed={reporte.miVoto === 'FALSO'}
        disabled={bloqueado}
        onClick={() => votar('FALSO')}
      >
        <ThumbsDown size={18} weight="bold" aria-hidden /> Es falso <span className="num">{reporte.falsos}</span>
      </button>
    </div>
  )
}

export default function ItemReporte({
  reporte,
  distancia,
  alCambiar,
}: {
  reporte: Reporte
  distancia?: number
  alCambiar: (r: Reporte) => void
}) {
  const r = reporte
  const detalle = [
    ARMAS[r.arma],
    MEDIO_FRASE[r.medio],
    r.elementoHurtado !== 'NINGUNO' ? `Se llevaron: ${ELEMENTOS[r.elementoHurtado].toLowerCase()}` : null,
  ]
    .filter(Boolean)
    .join(' · ')
  return (
    <li className="item-reporte">
      <div className="fila entre">
        <Link className="titulo" to={`/reportes/${r.id}`}>
          {TIPOS[r.tipoDelito]}
        </Link>
        <span className={`riesgo ${r.severidad}`}>{NIVELES[r.severidad]}</span>
      </div>
      <p className="small">{detalle}</p>
      {r.descripcion && <p className="muted">{r.descripcion}</p>}
      <div className="meta">
        <span>{hace(r.fechaHecho)}</span>
        <span>{r.barrio ?? `Comuna ${r.comuna}`}</span>
        {distancia !== undefined && <span className="num">a {metros(distancia)}</span>}
        <span className={`estado ${r.estado}`}>{ESTADOS[r.estado]}</span>
        {r.esMio && <span>Tu reporte</span>}
      </div>
      <div className="fila entre">
        <Votos reporte={r} alCambiar={alCambiar} />
        <Link className="btn texto chico" to={`/reportes/${r.id}`}>
          <ChatCircle size={18} weight="bold" aria-hidden /> Comentar
        </Link>
      </div>
    </li>
  )
}
