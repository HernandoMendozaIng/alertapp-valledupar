import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { api } from './api'
import { useAviso } from './aviso'
import type { Seguimiento } from './tipos'

const CADA_MS = 10000

interface ContextoSeguimiento {
  actual: Seguimiento | null
  iniciar: (minutos: number, lat: number, lng: number, rutaId?: number | null) => Promise<Seguimiento | null>
  detener: () => Promise<void>
  /** Vuelve a preguntar al servidor; lo usa el botón de pánico, que abre un seguimiento al activarse. */
  refrescar: () => void
}

const Contexto = createContext<ContextoSeguimiento | null>(null)

/**
 * HU 06 - Modo seguimiento. Mientras haya un seguimiento activo (un trayecto compartido o una alerta de pánico),
 * la app manda la posición del GPS cada 10 segundos para que los guardianes la vean moverse. Se apaga sola
 * al vencer o cuando el usuario la detiene, en cualquier pantalla de la app.
 */
export function ProveedorSeguimiento({ children }: { children: ReactNode }) {
  const aviso = useAviso()
  const [actual, setActual] = useState<Seguimiento | null>(null)
  const ultimoEnvio = useRef(0)

  const refrescar = useCallback(() => {
    api
      .get<Seguimiento | undefined>('/seguimiento')
      .then((s) => setActual(s && s.activo ? s : null))
      .catch(() => {})
  }, [])

  useEffect(refrescar, [refrescar])

  // Envío periódico de la posición mientras esté activo.
  useEffect(() => {
    if (!actual || !('geolocation' in navigator)) return
    const id = navigator.geolocation.watchPosition(
      (p) => {
        if (Date.now() - ultimoEnvio.current < CADA_MS) return
        ultimoEnvio.current = Date.now()
        api
          .put<Seguimiento>('/seguimiento/posicion', { latitud: p.coords.latitude, longitud: p.coords.longitude })
          .catch(() => refrescar())
      },
      () => {},
      { enableHighAccuracy: true, maximumAge: 5000, timeout: 20000 },
    )
    return () => navigator.geolocation.clearWatch(id)
  }, [actual, refrescar])

  // Al vencer, se apaga en la pantalla sin esperar al servidor.
  useEffect(() => {
    if (!actual) return
    const resta = new Date(actual.expiraEn).getTime() - Date.now()
    const t = window.setTimeout(() => {
      setActual(null)
      aviso('Tu ubicación dejó de compartirse')
    }, Math.max(0, resta))
    return () => window.clearTimeout(t)
  }, [actual, aviso])

  const valor = useMemo<ContextoSeguimiento>(
    () => ({
      actual,
      refrescar,
      iniciar: async (minutos, lat, lng, rutaId = null) => {
        try {
          const s = await api.post<Seguimiento>('/seguimiento', { minutos, latitud: lat, longitud: lng, rutaId })
          setActual(s)
          aviso(
            s.guardianesAvisados > 0
              ? `Compartiendo tu ubicación con ${s.guardianesAvisados} ${s.guardianesAvisados === 1 ? 'guardián' : 'guardianes'}`
              : 'Compartiendo tu ubicación. Agrega guardianes para que alguien la vea.',
          )
          return s
        } catch (e) {
          aviso((e as Error).message, 'error')
          return null
        }
      },
      detener: async () => {
        await api.post('/seguimiento/detener').catch(() => {})
        setActual(null)
        aviso('Dejaste de compartir tu ubicación')
      },
    }),
    [actual, refrescar, aviso],
  )

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>
}

export function useSeguimiento() {
  const c = useContext(Contexto)
  if (!c) throw new Error('useSeguimiento debe usarse dentro de ProveedorSeguimiento')
  return c
}

/** "quedan 42 min" */
export function restante(expira: string) {
  const min = Math.max(0, Math.ceil((new Date(expira).getTime() - Date.now()) / 60000))
  return min <= 1 ? 'queda menos de 1 min' : `quedan ${min} min`
}
