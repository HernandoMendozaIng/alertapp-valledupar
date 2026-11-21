import { expect, test } from '@playwright/test'
import { CIUDADANO, ingresar } from './ayudas'

test.describe('HU 03 Mapa y HU 05 Reporte guiado', () => {
  test.beforeEach(async ({ page }) => ingresar(page, CIUDADANO))

  test('UI-05 el mapa carga reportes, puntos calientes y el nivel de riesgo', async ({ page }) => {
    await expect(page.locator('.leaflet-container')).toBeVisible()
    await expect(page.getByText(/\d+ reportes en el mapa/)).toBeVisible()
    await expect(page.locator('.mapa-cabecera .riesgo')).toContainText('Riesgo')
    const filtro = page.getByRole('button', { name: 'A 200 m de mí' })
    await filtro.click()
    await expect(filtro).toHaveAttribute('aria-pressed', 'true')
  })

  test('UI-06 reporte guiado de principio a fin', async ({ page }) => {
    await page.getByRole('link', { name: 'Reportar' }).click()
    await page.locator('label.opcion', { hasText: 'Hurto sin violencia' }).click()
    await page.locator('label.opcion', { hasText: 'Sin arma' }).click()
    await page.locator('label.opcion', { hasText: 'A pie' }).first().click()
    await page.locator('label.opcion', { hasText: 'Celular' }).click()
    await page.getByLabel('Cuéntanos más (opcional)').fill('Prueba e2e: me arrebataron el celular en la esquina.')
    await page.getByRole('button', { name: 'Enviar reporte' }).click()
    await expect(page.getByRole('heading', { name: 'Hurto sin violencia' })).toBeVisible()
    await expect(page.getByText('Pendiente')).toBeVisible()
  })
})
