const CLAVE_TOKEN = 'alertapp.token'

export class ErrorApi extends Error {
  estado: number
  campos: Record<string, string>

  constructor(estado: number, mensaje: string, campos: Record<string, string> = {}) {
    super(mensaje)
    this.estado = estado
    this.campos = campos
  }
}

export function leerToken(): string | null {
  try {
    return localStorage.getItem(CLAVE_TOKEN)
  } catch {
    return null
  }
}

export function guardarToken(token: string | null) {
  try {
    if (token) localStorage.setItem(CLAVE_TOKEN, token)
    else localStorage.removeItem(CLAVE_TOKEN)
  } catch {
    // Sin almacenamiento local la sesión dura mientras la pestaña esté abierta.
  }
}

let alExpirar: () => void = () => {}
export function registrarAlExpirar(fn: () => void) {
  alExpirar = fn
}

async function solicitar<T>(metodo: string, ruta: string, cuerpo?: unknown): Promise<T> {
  const cabeceras: Record<string, string> = {}
  const token = leerToken()
  if (token) cabeceras.Authorization = `Bearer ${token}`
  let body: BodyInit | undefined
  if (cuerpo instanceof FormData) {
    body = cuerpo
  } else if (cuerpo !== undefined) {
    cabeceras['Content-Type'] = 'application/json'
    body = JSON.stringify(cuerpo)
  }
  let respuesta: Response
  try {
    respuesta = await fetch(`/api${ruta}`, { method: metodo, headers: cabeceras, body })
  } catch {
    throw new ErrorApi(0, 'Sin conexión con el servidor. Revisa tu internet e intenta de nuevo.')
  }
  if (respuesta.status === 401 && token) {
    alExpirar()
    throw new ErrorApi(401, 'Tu sesión expiró. Vuelve a ingresar.')
  }
  if (respuesta.status === 204) return undefined as T
  const datos = await respuesta.json().catch(() => null)
  if (!respuesta.ok) {
    throw new ErrorApi(respuesta.status, datos?.mensaje ?? 'No se pudo completar la acción.', datos?.campos ?? {})
  }
  return datos as T
}

export const api = {
  get: <T>(ruta: string) => solicitar<T>('GET', ruta),
  post: <T>(ruta: string, cuerpo?: unknown) => solicitar<T>('POST', ruta, cuerpo),
  put: <T>(ruta: string, cuerpo?: unknown) => solicitar<T>('PUT', ruta, cuerpo),
  patch: <T>(ruta: string, cuerpo?: unknown) => solicitar<T>('PATCH', ruta, cuerpo),
  delete: <T>(ruta: string) => solicitar<T>('DELETE', ruta),
}

/** Descarga un recurso protegido (foto o CSV) con el token y devuelve un Blob. */
export async function descargar(ruta: string): Promise<{ blob: Blob; nombre: string | null }> {
  const token = leerToken()
  const r = await fetch(`/api${ruta}`, { headers: token ? { Authorization: `Bearer ${token}` } : {} })
  if (!r.ok) throw new ErrorApi(r.status, 'No se pudo descargar el archivo.')
  const disposicion = r.headers.get('Content-Disposition')
  const nombre = disposicion?.match(/filename="?([^"]+)"?/)?.[1] ?? null
  return { blob: await r.blob(), nombre }
}

export function consulta(params: Record<string, string | number | undefined | null>) {
  const q = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') q.append(k, String(v))
  })
  const s = q.toString()
  return s ? `?${s}` : ''
}
