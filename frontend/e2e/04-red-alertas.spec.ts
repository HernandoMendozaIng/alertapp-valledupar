import { expect, test } from '@playwright/test'
import { api, CIUDADANO, crearReporte, ingresar, token } from './ayudas'

test.describe('HU 12 Red de guardianes, HU 11 Pánico y HU 02 Alertas en tiempo real', () => {
  test('UI-12 agrega y quita un guardián (máximo 3)', async ({ page }) => {
    await ingresar(page, CIUDADANO)
    await page.goto('/red')
    await expect(page.getByText('Marta Oñate')).toBeVisible()
    await page.getByLabel('Correo de la persona').fill('vecino3@alertapp.co')
    await page.getByRole('button', { name: 'Agregar guardián' }).click()
    await expect(page.getByText('3 de 3')).toBeVisible()
    await expect(page.getByLabel('Correo de la persona')).toBeDisabled()
    await page.getByRole('button', { name: 'Quitar a Yeimy Cuello' }).click()
    await expect(page.getByText('2 de 3')).toBeVisible()
  })

  test('UI-13 el guardián ve el pánico en vivo, con aviso y ubicación', async ({ page, request }) => {
    const luis = await token(request, CIUDADANO)
    await ingresar(page, 'vecino1@alertapp.co')
    await page.goto('/red')
    await expect(page.locator('.protegido', { hasText: 'Luis Ariza' })).toContainText('Sin alertas')

    const r = await api(request, luis, 'POST', '/alertas', { latitud: 10.4506, longitud: -73.2487 })
    const alerta = await r.json()
    try {
      // Llega por el canal en tiempo real, sin recargar.
      await expect(page.locator('.protegido', { hasText: 'Luis Ariza' })).toContainText('Botón de pánico', { timeout: 5000 })
      await expect(page.locator('.aviso')).toContainText('Luis Ariza activó el botón de pánico')
      await expect(page.locator('.leaflet-container')).toBeVisible()
      await expect(page.getByRole('link', { name: /Avisos, \d+ sin leer/ })).toBeVisible()
    } finally {
      await api(request, luis, 'POST', `/alertas/${alerta.id}/cancelar`)
    }
    await expect(page.locator('.protegido', { hasText: 'Luis Ariza' })).toContainText('Sin alertas', { timeout: 5000 })
  })

  test('UI-14 un reporte cercano llega como aviso en menos de 5 segundos', async ({ page, request }) => {
    const luis = await token(request, CIUDADANO)
    await ingresar(page, 'vecino4@alertapp.co')
    // Al abrir el mapa la app informa la ubicación (Primero de Mayo, simulada por el navegador).
    await expect(page.locator('.leaflet-container')).toBeVisible()
    await page.waitForTimeout(1500)
    const inicio = Date.now()
    await crearReporte(request, luis)
    await expect(page.locator('.aviso')).toContainText(/Atraco a \d+ m de ti/, { timeout: 5000 })
    const segundos = (Date.now() - inicio) / 1000
    test.info().annotations.push({ type: 'latencia', description: `${segundos.toFixed(2)} s` })
    expect(segundos).toBeLessThan(5)
    await page.getByRole('link', { name: /Avisos/ }).click()
    await expect(page.locator('.aviso-item.nuevo').first()).toContainText('de ti')
    await page.getByRole('button', { name: 'Marcar todo como leído' }).click()
    await expect(page.locator('.aviso-item.nuevo')).toHaveCount(0)
  })

  test('UI-15 botón de pánico con cuenta regresiva y cancelación', async ({ page }) => {
    await ingresar(page, CIUDADANO)
    await page.getByRole('button', { name: 'Activar botón de pánico' }).click()
    await expect(page.getByRole('dialog')).toContainText('Enviando alerta de pánico')
    await page.getByRole('button', { name: 'Cancelar' }).click()
    await expect(page.getByRole('dialog')).toBeHidden()
  })
})
