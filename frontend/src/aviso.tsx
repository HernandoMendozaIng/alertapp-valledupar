import { createContext, useCallback, useContext, useRef, useState, type ReactNode } from 'react'

type Tipo = 'ok' | 'error'
const Contexto = createContext<(mensaje: string, tipo?: Tipo) => void>(() => {})

export function ProveedorAviso({ children }: { children: ReactNode }) {
  const [aviso, setAviso] = useState<{ mensaje: string; tipo: Tipo; id: number } | null>(null)
  const temporizador = useRef<number | undefined>(undefined)

  const mostrar = useCallback((mensaje: string, tipo: Tipo = 'ok') => {
    window.clearTimeout(temporizador.current)
    setAviso({ mensaje, tipo, id: Date.now() })
    temporizador.current = window.setTimeout(() => setAviso(null), 4000)
  }, [])

  return (
    <Contexto.Provider value={mostrar}>
      {children}
      <div aria-live="polite" role="status">
        {aviso && (
          <div key={aviso.id} className={`aviso ${aviso.tipo === 'error' ? 'error' : ''}`}>
            {aviso.mensaje}
          </div>
        )}
      </div>
    </Contexto.Provider>
  )
}

export function useAviso() {
  return useContext(Contexto)
}
