import { createHmac } from 'node:crypto'
import { expect, type APIRequestContext, type Page } from '@playwright/test'

export const CLAVE = 'Valledupar2026'
export const CIUDADANO = 'ciudadano@alertapp.co'
export const AUTORIDAD = 'autoridad@alertapp.co'
const API = process.env.API_URL ?? 'http://localhost:8080'

/** Ingresa por la pantalla de acceso y espera a salir de ella. */
export async function ingresar(page: Page, correo: string, clave = CLAVE) {
  await page.goto('/ingreso')
  await page.getByLabel('Correo').fill(correo)
  await page.getByLabel('Contraseña').fill(clave)
  await page.getByRole('button', { name: 'Ingresar' }).click()
  await expect(page).not.toHaveURL(/\/ingreso/)
}

/** Token de sesión por la API, para preparar datos sin pasar por la interfaz. */
export async function token(request: APIRequestContext, correo: string, clave = CLAVE): Promise<string> {
  const r = await request.post(`${API}/api/auth/ingreso`, { data: { correo, clave } })
  expect(r.ok()).toBeTruthy()
  return (await r.json()).token
}

export async function api(request: APIRequestContext, tk: string, metodo: 'GET' | 'POST' | 'PUT' | 'DELETE', ruta: string, data?: unknown) {
  const r = await request.fetch(`${API}/api${ruta}`, { method: metodo, data, headers: { Authorization: `Bearer ${tk}` } })
  return r
}

/** Reporte nuevo en Primero de Mayo, hace 15 minutos. */
export async function crearReporte(request: APIRequestContext, tk: string, lat = 10.4545, lng = -73.2531) {
  const fecha = new Date(Date.now() - 15 * 60000).toISOString()
  const r = await api(request, tk, 'POST', '/reportes', {
    tipoDelito: 'ATRACO', arma: 'CORTOPUNZANTE', medio: 'MOTOCICLETA', elementoHurtado: 'CELULAR',
    descripcion: 'Prueba automática de interfaz', fechaHecho: fecha, latitud: lat, longitud: lng,
    comuna: 3, barrio: 'Primero de Mayo', anonimo: true,
  })
  expect(r.status()).toBe(201)
  return (await r.json()).id as number
}

/** Código TOTP (RFC 6238) a partir del secreto Base32, como lo calcula la app autenticadora. */
export function totp(secreto: string, desfase = 0) {
  const alfabeto = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'
  let bits = ''
  for (const c of secreto.replace(/\s/g, '')) bits += alfabeto.indexOf(c).toString(2).padStart(5, '0')
  const llave = Buffer.from(bits.match(/.{8}/g)!.map((b) => parseInt(b, 2)))
  const paso = Math.floor(Date.now() / 1000 / 30) + desfase
  const mensaje = Buffer.alloc(8)
  mensaje.writeBigUInt64BE(BigInt(paso))
  const h = createHmac('sha1', llave).update(mensaje).digest()
  const corte = h[h.length - 1] & 0x0f
  const n = ((h[corte] & 0x7f) << 24) | (h[corte + 1] << 16) | (h[corte + 2] << 8) | h[corte + 3]
  return String(n % 1_000_000).padStart(6, '0')
}
