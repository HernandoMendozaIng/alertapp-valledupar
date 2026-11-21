import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { api, leerToken } from './api'
import { useAviso } from './aviso'
import { useSesion } from './sesion'
import type { Bandeja, Notificacion } from './tipos'

type Oyente = (datos: unknown) => void

interface ContextoTiempoReal {
  conectado: boolean
  bandeja: Bandeja
  refrescar: () => void
  marcarLeida: (id: number) => void
  marcarTodas: () => void
  /** Se suscribe a un evento del servidor ("notificacion", "reporte", "ubicacion"). Devuelve la función para cancelar. */
  escuchar: (evento: string, fn: Oyente) => () => void
}

const Contexto = createContext<ContextoTiempoReal | null>(null)
const VACIA: Bandeja = { sinLeer: 0, notificaciones: [] }

/**
 * HU 02 - Canal en tiempo real. Abre /api/notificaciones/flujo (Server-Sent Events) con fetch para poder
 * mandar el token en la cabecera, lee los eventos a medida que llegan y se reconecta solo si se cae.
 * Si el usuario dio permiso, las alertas también salen como notificación del sistema operativo.
 */
export function ProveedorTiempoReal({ children }: { children: ReactNode }) {
  const { usuario } = useSesion()
  const aviso = useAviso()
  const [conectado, setConectado] = useState(false)
  const [bandeja, setBandeja] = useState<Bandeja>(VACIA)
  const oyentes = useRef(new Map<string, Set<Oyente>>())

  const refrescar = useCallback(() => {
    api.get<Bandeja>('/notificaciones').then(setBandeja).catch(() => {})
  }, [])

  const emitir = useCallback((evento: string, datos: unknown) => {
    oyentes.current.get(evento)?.forEach((fn) => fn(datos))
  }, [])

  useEffect(() => {
    if (!usuario) {
      setBandeja(VACIA)
      return
    }
    refrescar()
    let vivo = true
    let control: AbortController | null = null
    let espera = 1000

    async function conectar() {
      while (vivo) {
        control = new AbortController()
        try {
          const r = await fetch('/api/notificaciones/flujo', {
            headers: { Authorization: `Bearer ${leerToken()}`, Accept: 'text/event-stream' },
            signal: control.signal,
          })
          if (!r.ok || !r.body) throw new Error(String(r.status))
          setConectado(true)
          espera = 1000
          const lector = r.body.pipeThrough(new TextDecoderStream()).getReader()
          let pendiente = ''
          for (;;) {
            const { value, done } = await lector.read()
            if (done) break
            pendiente += value
            const bloques = pendiente.split('\n\n')
            pendiente = bloques.pop() ?? ''
            for (const bloque of bloques) {
              let evento = 'message'
              let datos = ''
              for (const linea of bloque.split('\n')) {
                if (linea.startsWith('event:')) evento = linea.slice(6).trim()
                else if (linea.startsWith('data:')) datos += linea.slice(5)
              }
              if (!datos) continue
              try {
                emitir(evento, JSON.parse(datos))
              } catch {
                // Evento mal formado: se ignora.
              }
            }
          }
        } catch {
          // Sin conexión o servidor reiniciando: se reintenta con espera creciente.
        }
        setConectado(false)
        if (!vivo) return
        await new Promise((ok) => setTimeout(ok, espera))
        espera = Math.min(espera * 2, 15000)
      }
    }

    conectar()
    return () => {
      vivo = false
      control?.abort()
    }
  }, [usuario, refrescar, emitir])

  // Cada notificación nueva entra a la bandeja, se muestra en pantalla y, si la app está en segundo plano, en el sistema.
  useEffect(() => {
    const fn = (d: unknown) => {
      const n = d as Notificacion
      setBandeja((b) => ({ sinLeer: b.sinLeer + 1, notificaciones: [n, ...b.notificaciones].slice(0, 30) }))
      aviso(n.titulo, n.tipo === 'PANICO_PROTEGIDO' ? 'error' : 'ok')
      if (n.tipo === 'PANICO_PROTEGIDO' && 'vibrate' in navigator) navigator.vibrate?.([300, 100, 300, 100, 300])
      if (document.hidden && 'Notification' in window && Notification.permission === 'granted') {
        new Notification(n.titulo, { body: n.cuerpo, tag: `alertapp-${n.id}` })
      }
    }
    const lista = oyentes.current.get('notificacion') ?? new Set<Oyente>()
    lista.add(fn)
    oyentes.current.set('notificacion', lista)
    return () => {
      lista.delete(fn)
    }
  }, [aviso])

  const valor = useMemo<ContextoTiempoReal>(
    () => ({
      conectado,
      bandeja,
      refrescar,
      marcarLeida: (id) => {
        setBandeja((b) => ({
          sinLeer: Math.max(0, b.sinLeer - (b.notificaciones.find((n) => n.id === id && !n.leida) ? 1 : 0)),
          notificaciones: b.notificaciones.map((n) => (n.id === id ? { ...n, leida: true } : n)),
        }))
        api.post(`/notificaciones/${id}/leida`).catch(() => {})
      },
      marcarTodas: () => {
        setBandeja((b) => ({ sinLeer: 0, notificaciones: b.notificaciones.map((n) => ({ ...n, leida: true })) }))
        api.post('/notificaciones/leidas').catch(() => {})
      },
      escuchar: (evento, fn) => {
        const lista = oyentes.current.get(evento) ?? new Set<Oyente>()
        lista.add(fn)
        oyentes.current.set(evento, lista)
        return () => {
          lista.delete(fn)
        }
      },
    }),
    [conectado, bandeja, refrescar],
  )

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>
}

export function useTiempoReal() {
  const c = useContext(Contexto)
  if (!c) throw new Error('useTiempoReal debe usarse dentro de ProveedorTiempoReal')
  return c
}

/** Atajo para suscribirse a un evento mientras el componente esté montado. */
export function useEvento(evento: string, fn: (datos: unknown) => void) {
  const { escuchar } = useTiempoReal()
  const ref = useRef(fn)
  ref.current = fn
  useEffect(() => escuchar(evento, (d) => ref.current(d)), [escuchar, evento])
}
