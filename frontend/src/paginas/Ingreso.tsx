import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { api, ErrorApi } from '../api'
import { useSesion } from '../sesion'
import type { Sesion } from '../tipos'
import Logo from '../componentes/Logo'
import ArteAcceso from '../componentes/ArteAcceso'

export default function Ingreso() {
  const { iniciar } = useSesion()
  const navegar = useNavigate()
  const desde = (useLocation().state as { desde?: string } | null)?.desde ?? '/'
  const [correo, setCorreo] = useState('')
  const [clave, setClave] = useState('')
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)

  async function enviar(e: FormEvent) {
    e.preventDefault()
    setError('')
    setEnviando(true)
    try {
      const s = await api.post<Sesion>('/auth/ingreso', { correo, clave })
      iniciar(s)
      navegar(s.usuario.rol === 'AUTORIDAD' && desde === '/' ? '/panel' : desde, { replace: true })
    } catch (err) {
      setError(err instanceof ErrorApi ? err.message : 'No se pudo ingresar')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="acceso">
      <ArteAcceso />
      <form className="acceso-form" onSubmit={enviar} noValidate>
        <div className="fila">
          <Logo tam={40} />
          <div>
            <h1>AlertApp</h1>
            <p className="muted small">Comunas 3 y 4 · Valledupar</p>
          </div>
        </div>
        <h2>Ingresa a tu cuenta</h2>
        {error && (
          <p className="alerta-error" role="alert">
            {error}
          </p>
        )}
        <div className="campo">
          <label htmlFor="correo">Correo</label>
          <input
            id="correo"
            type="email"
            autoComplete="email"
            inputMode="email"
            value={correo}
            onChange={(e) => setCorreo(e.target.value)}
            required
          />
        </div>
        <div className="campo">
          <label htmlFor="clave">Contraseña</label>
          <input
            id="clave"
            type="password"
            autoComplete="current-password"
            value={clave}
            onChange={(e) => setClave(e.target.value)}
            required
          />
        </div>
        <button className="btn bloque" disabled={enviando || !correo || !clave}>
          {enviando ? 'Ingresando…' : 'Ingresar'}
        </button>
        <p className="muted">
          ¿No tienes cuenta? <Link to="/registro">Regístrate</Link>
        </p>
      </form>
    </div>
  )
}
