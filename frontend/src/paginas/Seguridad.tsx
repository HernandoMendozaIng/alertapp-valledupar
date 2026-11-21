import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import QRCode from 'qrcode'
import { ArrowLeft, BellRinging, Key, ShieldCheck, UsersThree } from '@phosphor-icons/react'
import { api, ErrorApi } from '../api'
import { useAviso } from '../aviso'
import { useSesion } from '../sesion'
import type { Usuario } from '../tipos'

/**
 * HU 10 - Configuración y seguridad: alertas cercanas (HU 02), verificación en dos pasos,
 * cambio de contraseña y de correo, y acceso a los contactos de confianza (guardianes).
 */
export default function Seguridad() {
  return (
    <div className="pagina">
      <Link to="/perfil" className="btn texto chico" style={{ justifySelf: 'start' }}>
        <ArrowLeft size={16} weight="bold" aria-hidden /> Perfil
      </Link>
      <div className="encabezado">
        <h1>Seguridad y privacidad</h1>
        <p className="muted">Decide qué alertas recibes y protege el acceso a tu cuenta.</p>
      </div>
      <Alertas />
      <DosPasos />
      <Link to="/red" className="bloque enlace-bloque">
        <UsersThree size={24} weight="bold" aria-hidden />
        <span>
          <strong>Contactos de confianza</strong>
          <span className="small muted">Elige hasta 3 guardianes que reciban tus alertas de pánico.</span>
        </span>
      </Link>
      <CambiarClave />
      <CambiarCorreo />
    </div>
  )
}

function Alertas() {
  const { usuario, actualizar } = useSesion()
  const aviso = useAviso()
  const [activas, setActivas] = useState(usuario?.notificaciones ?? true)
  const [radio, setRadio] = useState(usuario?.radioAlertas ?? 500)
  const [permiso, setPermiso] = useState<NotificationPermission | 'no'>(
    'Notification' in window ? Notification.permission : 'no',
  )

  async function guardar(notificaciones: boolean, radioAlertas: number) {
    try {
      const u = await api.put<Usuario>('/auth/yo/preferencias', { notificaciones, radioAlertas })
      actualizar(u)
      aviso(notificaciones ? `Alertas activas a ${radioAlertas} m` : 'Alertas desactivadas')
    } catch (e) {
      aviso((e as Error).message, 'error')
    }
  }

  return (
    <section className="bloque" aria-labelledby="t-alertas">
      <h2 id="t-alertas" className="fila">
        <BellRinging size={22} weight="bold" aria-hidden /> Alertas cercanas
      </h2>
      <label className="interruptor">
        <input
          type="checkbox"
          role="switch"
          checked={activas}
          onChange={(e) => {
            setActivas(e.target.checked)
            guardar(e.target.checked, radio)
          }}
        />
        <span>Avisarme cuando reporten un hecho cerca de mí</span>
      </label>
      <div className="campo">
        <label htmlFor="s-radio">
          Distancia: <span className="num">{radio >= 1000 ? `${(radio / 1000).toFixed(1).replace('.', ',')} km` : `${radio} m`}</span>
        </label>
        <input
          id="s-radio"
          type="range"
          min={200}
          max={2000}
          step={100}
          value={radio}
          disabled={!activas}
          onChange={(e) => setRadio(Number(e.target.value))}
          onPointerUp={() => guardar(activas, radio)}
          onKeyUp={() => guardar(activas, radio)}
        />
        <span className="ayuda">Usamos tu última ubicación al abrir el mapa. No la mostramos a nadie.</span>
      </div>
      {permiso === 'default' && (
        <button
          className="btn secundario chico"
          style={{ justifySelf: 'start' }}
          onClick={() => Notification.requestPermission().then(setPermiso)}
        >
          Mostrar alertas aunque la app esté en segundo plano
        </button>
      )}
      {permiso === 'denied' && (
        <p className="small muted">El navegador bloqueó las notificaciones del sistema. Puedes activarlas en sus ajustes.</p>
      )}
    </section>
  )
}

function DosPasos() {
  const { usuario, actualizar } = useSesion()
  const aviso = useAviso()
  const [config, setConfig] = useState<{ secreto: string; uri: string } | null>(null)
  const [qr, setQr] = useState('')
  const [codigo, setCodigo] = useState('')
  const [clave, setClave] = useState('')
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    if (!config) return
    QRCode.toDataURL(config.uri, { margin: 1, width: 200, color: { dark: '#15191d', light: '#ffffff' } })
      .then(setQr)
      .catch(() => setQr(''))
  }, [config])

  async function preparar() {
    setError('')
    try {
      setConfig(await api.post('/auth/2fa/preparar'))
    } catch (e) {
      setError((e as Error).message)
    }
  }

  async function activar(e: FormEvent) {
    e.preventDefault()
    setError('')
    setEnviando(true)
    try {
      actualizar(await api.post<Usuario>('/auth/2fa/activar', { codigo }))
      setConfig(null)
      setCodigo('')
      aviso('Verificación en dos pasos activada')
    } catch (err) {
      setError(err instanceof ErrorApi ? err.message : 'No se pudo activar')
    } finally {
      setEnviando(false)
    }
  }

  async function desactivar(e: FormEvent) {
    e.preventDefault()
    setError('')
    setEnviando(true)
    try {
      actualizar(await api.post<Usuario>('/auth/2fa/desactivar', { clave, codigo }))
      setClave('')
      setCodigo('')
      aviso('Verificación en dos pasos desactivada')
    } catch (err) {
      setError(err instanceof ErrorApi ? err.message : 'No se pudo desactivar')
    } finally {
      setEnviando(false)
    }
  }

  const campoCodigo = (
    <div className="campo">
      <label htmlFor="s-codigo">Código de 6 dígitos de la app</label>
      <input
        id="s-codigo"
        inputMode="numeric"
        autoComplete="one-time-code"
        pattern="\d{6}"
        maxLength={6}
        className="num codigo"
        value={codigo}
        onChange={(e) => setCodigo(e.target.value.replace(/\D/g, ''))}
      />
    </div>
  )

  return (
    <section className="bloque" aria-labelledby="t-2fa">
      <div className="fila entre">
        <h2 id="t-2fa" className="fila">
          <ShieldCheck size={22} weight="bold" aria-hidden /> Verificación en dos pasos
        </h2>
        <span className={`estado ${usuario?.dosFactores ? 'VALIDADO' : 'PENDIENTE'}`}>
          {usuario?.dosFactores ? 'Activa' : 'Inactiva'}
        </span>
      </div>
      <p className="small muted">
        Además de tu contraseña, te pediremos un código que cambia cada 30 segundos en una app autenticadora (Google
        Authenticator, Microsoft Authenticator o similar).
      </p>
      {error && (
        <p className="alerta-error" role="alert">
          {error}
        </p>
      )}

      {!usuario?.dosFactores && !config && (
        <button className="btn" style={{ justifySelf: 'start' }} onClick={preparar}>
          Activar
        </button>
      )}

      {!usuario?.dosFactores && config && (
        <form onSubmit={activar} className="dos-pasos">
          <ol className="pasos small">
            <li>Abre tu app autenticadora y escanea este código.</li>
            <li>Escribe el código de 6 dígitos que te muestra.</li>
          </ol>
          <div className="qr">
            {qr ? <img src={qr} width={200} height={200} alt="Código QR para la app autenticadora" /> : <div className="esqueleto" style={{ width: 200, height: 200 }} />}
            <details>
              <summary className="small">No puedo escanear</summary>
              <p className="small">Escribe esta clave en la app:</p>
              <code className="secreto">{config.secreto.match(/.{1,4}/g)?.join(' ')}</code>
            </details>
          </div>
          {campoCodigo}
          <div className="fila">
            <button className="btn" disabled={codigo.length !== 6 || enviando}>
              {enviando ? 'Verificando…' : 'Confirmar y activar'}
            </button>
            <button type="button" className="btn texto" onClick={() => setConfig(null)}>
              Cancelar
            </button>
          </div>
        </form>
      )}

      {usuario?.dosFactores && (
        <form onSubmit={desactivar} style={{ display: 'grid', gap: 12 }}>
          <div className="campo">
            <label htmlFor="s-clave-2fa">Contraseña</label>
            <input id="s-clave-2fa" type="password" autoComplete="current-password" value={clave} onChange={(e) => setClave(e.target.value)} />
          </div>
          {campoCodigo}
          <button className="btn secundario" style={{ justifySelf: 'start' }} disabled={!clave || codigo.length !== 6 || enviando}>
            Desactivar
          </button>
        </form>
      )}
    </section>
  )
}

function CambiarClave() {
  const aviso = useAviso()
  const [actual, setActual] = useState('')
  const [nueva, setNueva] = useState('')
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [error, setError] = useState('')

  async function enviar(e: FormEvent) {
    e.preventDefault()
    setErrores({})
    setError('')
    try {
      await api.put('/auth/yo/clave', { actual, nueva })
      setActual('')
      setNueva('')
      aviso('Contraseña actualizada')
    } catch (err) {
      if (err instanceof ErrorApi) {
        setErrores(err.campos)
        if (!Object.keys(err.campos).length) setError(err.message)
      }
    }
  }

  return (
    <form className="bloque" onSubmit={enviar} noValidate aria-labelledby="t-clave">
      <h2 id="t-clave" className="fila">
        <Key size={22} weight="bold" aria-hidden /> Cambiar contraseña
      </h2>
      {error && (
        <p className="alerta-error" role="alert">
          {error}
        </p>
      )}
      <div className="campo">
        <label htmlFor="c-actual">Contraseña actual</label>
        <input id="c-actual" type="password" autoComplete="current-password" value={actual} onChange={(e) => setActual(e.target.value)} />
      </div>
      <div className="campo">
        <label htmlFor="c-nueva">Contraseña nueva</label>
        <input
          id="c-nueva"
          type="password"
          autoComplete="new-password"
          value={nueva}
          onChange={(e) => setNueva(e.target.value)}
          aria-invalid={Boolean(errores.nueva)}
        />
        <span className="ayuda">Mínimo 8 caracteres, con letras y números.</span>
        {errores.nueva && <span className="error">{errores.nueva}</span>}
      </div>
      <button className="btn" style={{ justifySelf: 'start' }} disabled={!actual || !nueva}>
        Cambiar contraseña
      </button>
    </form>
  )
}

function CambiarCorreo() {
  const { usuario, actualizar } = useSesion()
  const aviso = useAviso()
  const [correo, setCorreo] = useState('')
  const [clave, setClave] = useState('')
  const [error, setError] = useState('')

  async function enviar(e: FormEvent) {
    e.preventDefault()
    setError('')
    try {
      actualizar(await api.put<Usuario>('/auth/yo/correo', { correo, clave }))
      setCorreo('')
      setClave('')
      aviso('Correo actualizado')
    } catch (err) {
      setError(err instanceof ErrorApi ? (Object.values(err.campos)[0] ?? err.message) : 'No se pudo cambiar')
    }
  }

  return (
    <form className="bloque" onSubmit={enviar} noValidate aria-labelledby="t-correo">
      <h2 id="t-correo">Cambiar correo</h2>
      <p className="small muted">Ahora ingresas con {usuario?.correo}.</p>
      {error && (
        <p className="alerta-error" role="alert">
          {error}
        </p>
      )}
      <div className="campo">
        <label htmlFor="m-correo">Correo nuevo</label>
        <input id="m-correo" type="email" inputMode="email" autoComplete="email" value={correo} onChange={(e) => setCorreo(e.target.value)} />
      </div>
      <div className="campo">
        <label htmlFor="m-clave">Contraseña</label>
        <input id="m-clave" type="password" autoComplete="current-password" value={clave} onChange={(e) => setClave(e.target.value)} />
      </div>
      <button className="btn" style={{ justifySelf: 'start' }} disabled={!correo || !clave}>
        Cambiar correo
      </button>
    </form>
  )
}
