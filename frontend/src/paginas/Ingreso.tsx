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
  // HU 10: si la cuenta tiene verificación en dos pasos, el servidor devuelve un desafío y se pide el código.
  const [desafio, setDesafio] = useState<string | null>(null)
  const [codigo, setCodigo] = useState('')

  function entrar(s: Sesion) {
    iniciar(s)
    navegar(s.usuario.rol === 'AUTORIDAD' && desde === '/' ? '/panel' : desde, { replace: true })
  }

  async function enviar(e: FormEvent) {
    e.preventDefault()
    setError('')
    setEnviando(true)
    try {
      if (desafio) {
        entrar(await api.post<Sesion>('/auth/ingreso/2fa', { desafio, codigo }))
        return
      }
      const s = await api.post<Sesion>('/auth/ingreso', { correo, clave })
      if (s.requiereSegundoFactor && s.desafio) {
        setDesafio(s.desafio)
        return
      }
      entrar(s)
    } catch (err) {
      setError(err instanceof ErrorApi ? err.message : 'No se pudo ingresar')
      if (err instanceof ErrorApi && desafio && err.message.includes('venció')) {
        setDesafio(null)
        setCodigo('')
      }
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
        <h2>{desafio ? 'Verificación en dos pasos' : 'Ingresa a tu cuenta'}</h2>
        {error && (
          <p className="alerta-error" role="alert">
            {error}
          </p>
        )}
        {desafio ? (
          <>
            <p className="muted">Escribe el código de 6 dígitos que muestra tu app autenticadora.</p>
            <div className="campo">
              <label htmlFor="codigo">Código</label>
              <input
                id="codigo"
                className="num codigo"
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                autoFocus
                value={codigo}
                onChange={(e) => setCodigo(e.target.value.replace(/\D/g, ''))}
              />
            </div>
            <button className="btn bloque" disabled={enviando || codigo.length !== 6}>
              {enviando ? 'Verificando…' : 'Verificar'}
            </button>
            <button
              type="button"
              className="btn texto"
              onClick={() => {
                setDesafio(null)
                setCodigo('')
              }}
            >
              Volver
            </button>
          </>
        ) : (
          <>
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
        <Link to="/recuperar" className="small">
          ¿Olvidaste tu contraseña?
        </Link>
          </>
        )}
        <p className="muted">
          ¿No tienes cuenta? <Link to="/registro">Regístrate</Link>
        </p>
      </form>
    </div>
  )
}
