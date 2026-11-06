import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, ErrorApi } from '../api'
import { useSesion } from '../sesion'
import type { Sesion } from '../tipos'
import Logo from '../componentes/Logo'
import ArteAcceso from '../componentes/ArteAcceso'

export default function Registro() {
  const { iniciar } = useSesion()
  const navegar = useNavigate()
  const [datos, setDatos] = useState({
    nombre: '',
    correo: '',
    clave: '',
    telefono: '',
    comuna: '3',
    aceptoPolitica: false,
  })
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)

  function cambiar(campo: keyof typeof datos, valor: string | boolean) {
    setDatos((d) => ({ ...d, [campo]: valor }))
    setErrores((e) => ({ ...e, [campo]: '' }))
  }

  async function enviar(e: FormEvent) {
    e.preventDefault()
    setError('')
    setEnviando(true)
    try {
      const s = await api.post<Sesion>('/auth/registro', { ...datos, comuna: Number(datos.comuna) })
      iniciar(s)
      navegar('/', { replace: true })
    } catch (err) {
      if (err instanceof ErrorApi) {
        setErrores(err.campos)
        setError(err.message)
      }
    } finally {
      setEnviando(false)
    }
  }

  const campo = (id: keyof typeof datos, etiqueta: string, props: Record<string, unknown> = {}, ayuda?: string) => (
    <div className="campo">
      <label htmlFor={id}>{etiqueta}</label>
      <input
        id={id}
        value={String(datos[id])}
        onChange={(e) => cambiar(id, e.target.value)}
        aria-invalid={Boolean(errores[id])}
        aria-describedby={`${id}-nota`}
        {...props}
      />
      {errores[id] ? (
        <span className="error" id={`${id}-nota`}>
          {errores[id]}
        </span>
      ) : (
        ayuda && (
          <span className="ayuda" id={`${id}-nota`}>
            {ayuda}
          </span>
        )
      )}
    </div>
  )

  return (
    <div className="acceso">
      <ArteAcceso />
      <form className="acceso-form" onSubmit={enviar} noValidate>
        <div className="fila">
          <Logo tam={40} />
          <h1>Crea tu cuenta</h1>
        </div>
        {error && (
          <p className="alerta-error" role="alert">
            {error}
          </p>
        )}
        {campo('nombre', 'Nombre', { autoComplete: 'name', required: true })}
        {campo('correo', 'Correo', { type: 'email', autoComplete: 'email', inputMode: 'email', required: true })}
        {campo(
          'clave',
          'Contraseña',
          { type: 'password', autoComplete: 'new-password', required: true },
          'Mínimo 8 caracteres, con letras y números.',
        )}
        {campo(
          'telefono',
          'Celular (opcional)',
          { type: 'tel', autoComplete: 'tel-national', inputMode: 'numeric', maxLength: 10 },
          'Solo lo ve la autoridad si activas el botón de pánico.',
        )}
        <div className="campo">
          <label htmlFor="comuna">¿En qué comuna vives o transitas?</label>
          <select id="comuna" value={datos.comuna} onChange={(e) => cambiar('comuna', e.target.value)}>
            <option value="3">Comuna 3</option>
            <option value="4">Comuna 4</option>
            <option value="1">Comuna 1</option>
            <option value="2">Comuna 2</option>
            <option value="5">Comuna 5</option>
            <option value="6">Comuna 6</option>
          </select>
        </div>
        <label className="check">
          <input
            type="checkbox"
            checked={datos.aceptoPolitica}
            onChange={(e) => cambiar('aceptoPolitica', e.target.checked)}
            aria-invalid={Boolean(errores.aceptoPolitica)}
          />
          <span>
            Autorizo el tratamiento de mis datos según la Ley 1581 de 2012. Mis reportes son anónimos por defecto.
            {errores.aceptoPolitica && (
              <span className="error" style={{ display: 'block' }}>
                {errores.aceptoPolitica}
              </span>
            )}
          </span>
        </label>
        <button className="btn bloque" disabled={enviando}>
          {enviando ? 'Creando cuenta…' : 'Crear cuenta'}
        </button>
        <p className="muted">
          ¿Ya tienes cuenta? <Link to="/ingreso">Ingresa</Link>
        </p>
      </form>
    </div>
  )
}
