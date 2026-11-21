import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, ErrorApi } from '../api'
import { useAviso } from '../aviso'
import Logo from '../componentes/Logo'
import ArteAcceso from '../componentes/ArteAcceso'

/** HU 01 - Recuperación de contraseña con un código de 6 dígitos que llega por correo. */
export default function Recuperar() {
  const aviso = useAviso()
  const navegar = useNavigate()
  const [paso, setPaso] = useState<1 | 2>(1)
  const [correo, setCorreo] = useState('')
  const [codigo, setCodigo] = useState('')
  const [nueva, setNueva] = useState('')
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)

  async function pedir(e: FormEvent) {
    e.preventDefault()
    setError('')
    setEnviando(true)
    try {
      await api.post('/auth/recuperar', { correo })
      setPaso(2)
    } catch (err) {
      setError(err instanceof ErrorApi ? (Object.values(err.campos)[0] ?? err.message) : 'No se pudo enviar')
    } finally {
      setEnviando(false)
    }
  }

  async function confirmar(e: FormEvent) {
    e.preventDefault()
    setError('')
    setEnviando(true)
    try {
      await api.post('/auth/recuperar/confirmar', { correo, codigo, nueva })
      aviso('Contraseña restablecida. Ya puedes ingresar.')
      navegar('/ingreso', { replace: true })
    } catch (err) {
      setError(err instanceof ErrorApi ? (Object.values(err.campos)[0] ?? err.message) : 'No se pudo restablecer')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="acceso">
      <ArteAcceso />
      <form className="acceso-form" onSubmit={paso === 1 ? pedir : confirmar} noValidate>
        <div className="fila">
          <Logo tam={40} />
          <div>
            <h1>AlertApp</h1>
            <p className="muted small">Comunas 3 y 4 · Valledupar</p>
          </div>
        </div>
        <h2>Recuperar contraseña</h2>
        {error && (
          <p className="alerta-error" role="alert">
            {error}
          </p>
        )}
        {paso === 1 ? (
          <>
            <p className="muted">Te enviaremos un código de 6 dígitos al correo con el que te registraste.</p>
            <div className="campo">
              <label htmlFor="r-correo">Correo</label>
              <input id="r-correo" type="email" inputMode="email" autoComplete="email" value={correo} onChange={(e) => setCorreo(e.target.value)} />
            </div>
            <button className="btn bloque" disabled={!correo || enviando}>
              {enviando ? 'Enviando…' : 'Enviar código'}
            </button>
          </>
        ) : (
          <>
            <p className="muted">
              Si {correo} tiene una cuenta, le llegó un código que vence en 15 minutos. Escríbelo y crea una contraseña nueva.
            </p>
            <div className="campo">
              <label htmlFor="r-codigo">Código</label>
              <input
                id="r-codigo"
                className="num codigo"
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                value={codigo}
                onChange={(e) => setCodigo(e.target.value.replace(/\D/g, ''))}
              />
            </div>
            <div className="campo">
              <label htmlFor="r-nueva">Contraseña nueva</label>
              <input id="r-nueva" type="password" autoComplete="new-password" value={nueva} onChange={(e) => setNueva(e.target.value)} />
              <span className="ayuda">Mínimo 8 caracteres, con letras y números.</span>
            </div>
            <button className="btn bloque" disabled={codigo.length !== 6 || !nueva || enviando}>
              {enviando ? 'Guardando…' : 'Restablecer contraseña'}
            </button>
            <button type="button" className="btn texto" onClick={() => setPaso(1)}>
              No me llegó el código
            </button>
          </>
        )}
        <p className="muted">
          <Link to="/ingreso">Volver a ingresar</Link>
        </p>
      </form>
    </div>
  )
}
