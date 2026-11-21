import { expect, test } from '@playwright/test'
import { CIUDADANO, ingresar } from './ayudas'

test.describe('HU 04 Rutas seguras y HU 06 Rutas frecuentes', () => {
  test.beforeEach(async ({ page }) => {
    await ingresar(page, CIUDADANO)
    await page.getByRole('link', { name: 'Rutas' }).click()
  })

  test('UI-07 calcula la ruta segura de una ruta frecuente y la compara con la corta', async ({ page }) => {
    await page.getByRole('button', { name: 'Casa al trabajo' }).click()
    const panel = page.locator('.panel-ruta')
    await expect(panel).toContainText(/La ruta segura (evita|pasa lejos|se aleja)|ya es la más segura/)
    await expect(panel.getByText('Segura', { exact: true })).toBeVisible()
    await expect(panel.getByText('Más corta', { exact: true })).toBeVisible()
    await expect(page.locator('path.leaflet-interactive').first()).toBeVisible()
  })

  test('UI-08 cambiar de modo vuelve a calcular la ruta', async ({ page }) => {
    await page.getByRole('button', { name: 'Casa al trabajo' }).click()
    await expect(page.locator('.panel-ruta h2')).toContainText('A pie')
    await page.getByRole('button', { name: 'Carro o moto' }).click()
    await expect(page.locator('.panel-ruta h2')).toContainText('Carro o moto')
  })

  test('UI-09 marca un destino tocando el mapa', async ({ page }) => {
    const mapa = page.locator('.leaflet-container')
    const caja = (await mapa.boundingBox())!
    await mapa.click({ position: { x: caja.width * 0.7, y: caja.height * 0.8 } })
    await expect(page.locator('.panel-ruta, .mapa-cabecera .alerta-error')).toBeVisible()
  })

  test('UI-10 comparte el trayecto con los guardianes y lo detiene', async ({ page }) => {
    await page.getByRole('button', { name: 'Casa al trabajo' }).click()
    await page.getByLabel('Tiempo para compartir').selectOption('15')
    await page.getByRole('button', { name: 'Compartir trayecto' }).click()
    const banda = page.locator('.banda-seguimiento')
    await expect(banda).toContainText('Compartiendo tu ubicación · Casa al trabajo')
    await banda.getByRole('button', { name: 'Detener' }).click()
    await expect(banda).toBeHidden()
  })

  test('UI-11 guarda, renombra y borra una ruta frecuente', async ({ page }) => {
    await page.getByRole('button', { name: 'Casa al trabajo' }).click()
    await page.getByRole('button', { name: 'Editar ruta' }).click()
    // Se guarda como nueva limpiando la elegida: se marca un destino nuevo.
    await page.getByRole('button', { name: 'Limpiar' }).click()
    const mapa = page.locator('.leaflet-container')
    const caja = (await mapa.boundingBox())!
    await mapa.click({ position: { x: caja.width * 0.6, y: caja.height * 0.75 } })
    await page.getByRole('button', { name: 'Guardar ruta' }).click()
    await page.getByLabel('Nombre de la ruta').fill('Ruta de prueba e2e')
    await page.getByRole('button', { name: 'Guardar', exact: true }).click()
    await expect(page.getByRole('button', { name: 'Ruta de prueba e2e', exact: true })).toBeVisible()
    await page.getByRole('button', { name: 'Editar ruta' }).click()
    await page.getByLabel('Nombre de la ruta').fill('Ruta e2e renombrada')
    await page.getByRole('button', { name: 'Guardar', exact: true }).click()
    await expect(page.getByRole('button', { name: 'Ruta e2e renombrada', exact: true })).toBeVisible()
    await page.getByRole('button', { name: 'Editar ruta' }).click()
    await page.getByRole('button', { name: 'Borrar Ruta e2e renombrada' }).click()
    await expect(page.getByRole('button', { name: 'Ruta e2e renombrada', exact: true })).toBeHidden()
  })
})
