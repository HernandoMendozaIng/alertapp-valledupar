import { useEffect, useRef, useState } from 'react'
import { Siren } from '@phosphor-icons/react'
import { api } from '../api'
import { useAviso } from '../aviso'
import type { Alerta } from '../tipos'
import type { Ubicacion } from '../useUbicacion'

const SEGUNDOS = 5

/**
 * HU 11 - Botón de pánico. Al tocarlo empieza una cuenta de 5 segundos para cancelar
 * (criterio de aceptación); si no se cancela, la alerta se envía con la ubicación GPS.
 */
export default function BotonPanico({ ubicacion }: { ubicacion: Ubicacion }) {
  const aviso = useAviso()
  const [fase, setFase] = useState<'reposo' | 'cuenta' | 'enviando' | 'activa'>('reposo')
  const [restante, setRestante] = useState(SEGUNDOS)
  const [alerta, setAlerta] = useState<Alerta | null>(null)
  const intervalo = useRef<number | undefined>(undefined)
  const cancelarRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    api
      .get<Alerta[]>('/alertas/mias')
      .then((l) => {
        const activa = l.find((a) => a.estado === 'ACTIVA')
        if (activa) {
          setAlerta(activa)
          setFase('activa')
        }
      })
      .catch(() => {})
  }, [])

  useEffect(() => {
    if (fase !== 'cuenta') return
    cancelarRef.current?.focus()
    intervalo.current = window.setInterval(() => {
      setRestante((s) => s - 1)
    }, 1000)
    return () => window.clearInterval(intervalo.current)
  }, [fase])

  useEffect(() => {
    if (fase === 'cuenta' && restante <= 0) {
      window.clearInterval(intervalo.current)
      setFase('enviando')
      api
        .post<Alerta>('/alertas', { latitud: ubicacion.lat, longitud: ubicacion.lng })
        .then((a) => {
          setAlerta(a)
          setFase('activa')
          if ('vibrate' in navigator) navigator.vibrate?.([120, 60, 120])
        })
        .catch((e) => {
          aviso(e.message, 'error')
          setFase('reposo')
        })
    }
  }, [restante, fase, ubicacion, aviso])

  function iniciar() {
    setRestante(SEGUNDOS)
    setFase('cuenta')
  }

  function cancelarCuenta() {
    window.clearInterval(intervalo.current)
    setFase('reposo')
  }

  async function cancelarAlerta() {
    if (!alerta) return
    try {
      await api.post<Alerta>(`/alertas/${alerta.id}/cancelar`)
      aviso('Alerta cancelada')
    } catch (e) {
      aviso((e as Error).message, 'error')
    }
    setAlerta(null)
    setFase('reposo')
  }

  return (
    <>
      <button className="panico" onClick={iniciar} aria-label="Activar botón de pánico">
        <Siren size={28} weight="fill" aria-hidden />
        SOS
      </button>

      {fase !== 'reposo' && (
        <div className="hoja-fondo" role="dialog" aria-modal="true" aria-labelledby="titulo-panico">
          <div className="hoja">
            {fase === 'cuenta' && (
              <>
                <h2 id="titulo-panico">Enviando alerta de pánico</h2>
                <p className="cuenta num" aria-live="assertive">
                  {restante}
                </p>
                <p className="muted">
                  La autoridad recibirá tu ubicación{ubicacion.real ? '' : ' aproximada (no se pudo leer el GPS)'}.
                  Si fue un error, cancela ahora.
                </p>
                <button ref={cancelarRef} className="btn secundario bloque" onClick={cancelarCuenta}>
                  Cancelar
                </button>
              </>
            )}
            {fase === 'enviando' && (
              <>
                <h2 id="titulo-panico">Enviando…</h2>
                <p className="muted">Registrando tu ubicación.</p>
              </>
            )}
            {fase === 'activa' && alerta && (
              <>
                <h2 id="titulo-panico">Alerta enviada</h2>
                <p>
                  La autoridad ya ve tu alerta en su panel. Busca un lugar concurrido y, si puedes, llama al{' '}
                  <a href="tel:123">123</a>.
                </p>
                <a className="btn peligro bloque" href="tel:123">
                  Llamar al 123
                </a>
                <button className="btn secundario bloque" onClick={cancelarAlerta}>
                  Ya estoy a salvo, cancelar alerta
                </button>
                <button className="btn texto" onClick={() => setFase('reposo')}>
                  Ocultar
                </button>
              </>
            )}
          </div>
        </div>
      )}
    </>
  )
}
