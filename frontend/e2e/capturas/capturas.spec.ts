import { expect, test, type Page } from '@playwright/test'
import { api, AUTORIDAD, CIUDADANO, ingresar, token } from '../ayudas'

/**
 * Genera las capturas de pantalla de docs/capturas para el README y el documento del proyecto.
 * Uso: npm run capturas (con los tres servicios corriendo).
 */
const DESTINO = '../docs/capturas'

async function foto(page: Page, nombre: string) {
  await page.waitForTimeout(900)
  await page.screenshot({ path: `${DESTINO}/${nombre}.png` })
}

test('capturas del ciudadano', async ({ page }) => {
  await page.goto('/ingreso')
  await foto(page, '01-ingreso')
  await ingresar(page, CIUDADANO)
  await expect(page.getByText(/\d+ reportes en el mapa/)).toBeVisible()
  await foto(page, '02-mapa')
  await page.getByRole('link', { name: 'Reportar' }).click()
  await foto(page, '03-reportar')
  await page.getByRole('link', { name: 'Rutas' }).click()
  await page.getByRole('button', { name: 'Casa al trabajo' }).click()
  await expect(page.locator('.panel-ruta')).toContainText('La ruta segura')
  await foto(page, '04-ruta-segura')
  await page.getByRole('link', { name: 'Comunidad' }).click()
  await foto(page, '05-comunidad')
  await page.goto('/red')
  await foto(page, '06-red-guardianes')
  await page.goto('/perfil/seguridad')
  await foto(page, '07-seguridad')
  await page.goto('/metricas')
  await expect(page.getByRole('heading', { name: '¿A qué hora pasan?' })).toBeVisible()
  await foto(page, '08-metricas')
})

test('capturas del guardián en una alerta de pánico', async ({ page, request }) => {
  const luis = await token(request, CIUDADANO)
  const alerta = await (await api(request, luis, 'POST', '/alertas', { latitud: 10.4506, longitud: -73.2487 })).json()
  try {
    await ingresar(page, 'vecino1@alertapp.co')
    await page.goto('/red')
    await expect(page.locator('.protegido', { hasText: 'Luis Ariza' })).toContainText('Botón de pánico')
    await foto(page, '09-guardian-panico')
    await page.goto('/avisos')
    await foto(page, '10-avisos')
  } finally {
    await api(request, luis, 'POST', `/alertas/${alerta.id}/cancelar`)
  }
})

test.describe('escritorio', () => {
  test.use({ viewport: { width: 1440, height: 900 }, isMobile: false, hasTouch: false, deviceScaleFactor: 1 })

  test('capturas de la autoridad', async ({ page }) => {
    await ingresar(page, AUTORIDAD)
    await expect(page.getByRole('heading', { name: 'Panel de autoridad' })).toBeVisible()
    await foto(page, '11-panel-autoridad')
    await page.goto('/panel/informe')
    await foto(page, '12-informe-semanal')
  })

  test('captura de la ruta segura en escritorio', async ({ page }) => {
    await ingresar(page, CIUDADANO)
    await page.goto('/rutas')
    await page.getByRole('button', { name: 'Casa al trabajo' }).click()
    await expect(page.locator('.panel-ruta')).toContainText('La ruta segura')
    await foto(page, '13-ruta-segura-escritorio')
  })
})
