import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, guardarToken, leerToken, registrarAlExpirar } from './api'
import type { Sesion, Usuario } from './tipos'

interface ContextoSesion {
  usuario: Usuario | null
  cargando: boolean
  iniciar: (s: Sesion) => void
  salir: () => void
  actualizar: (u: Usuario) => void
}

const Contexto = createContext<ContextoSesion | null>(null)

export function ProveedorSesion({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null)
  const [cargando, setCargando] = useState(() => leerToken() !== null)

  const salir = useCallback(() => {
    guardarToken(null)
    setUsuario(null)
  }, [])

  useEffect(() => {
    registrarAlExpirar(salir)
    if (!leerToken()) return
    api
      .get<Usuario>('/auth/yo')
      .then(setUsuario)
      .catch(() => salir())
      .finally(() => setCargando(false))
  }, [salir])

  const valor = useMemo<ContextoSesion>(
    () => ({
      usuario,
      cargando,
      iniciar: (s) => {
        guardarToken(s.token)
        setUsuario(s.usuario)
      },
      salir,
      actualizar: setUsuario,
    }),
    [usuario, cargando, salir],
  )

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>
}

export function useSesion() {
  const c = useContext(Contexto)
  if (!c) throw new Error('useSesion debe usarse dentro de ProveedorSesion')
  return c
}
