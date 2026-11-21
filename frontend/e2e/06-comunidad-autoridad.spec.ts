import { expect, test } from '@playwright/test'
import { api, AUTORIDAD, CIUDADANO, crearReporte, ingresar, token } from './ayudas'

test.describe('HU 07 Comunidad, HU 08 Métricas, HU 09 y HU 13 Autoridad', () => {
  test('UI-19 la comunidad lista reportes cercanos y permite votar', async ({ page, request }) => {
    const id = await crearReporte(request, await token(request, 'vecino6@alertapp.co'))
    await ingresar(page, CIUDADANO)
    await page.goto(`/reportes/${id}`)
    const confirmo = page.getByRole('button', { name: /Confirmo/ })
    await confirmo.click()
    await expect(confirmo).toHaveAttribute('aria-pressed', 'true')
    await page.goto('/comunidad')
    await expect(page.locator('.item-reporte').first()).toBeVisible()
  })

  test('UI-20 métricas con filtros sin recargar la página', async ({ page }) => {
    await ingresar(page, CIUDADANO)
    await page.getByRole('link', { name: 'Perfil' }).click()
    await page.getByRole('link', { name: /Métricas de seguridad/ }).click()
    await expect(page.getByRole('heading', { name: '¿A qué hora pasan?' })).toBeVisible()
    await page.getByLabel('Comuna').selectOption('4')
    await expect(page.getByRole('heading', { name: '¿Qué pasa más?' })).toBeVisible()
  })

  test('UI-21 la autoridad cambia el estado y el ciudadano recibe el aviso', async ({ page, request }) => {
    const luis = await token(request, CIUDADANO)
    const id = await crearReporte(request, luis)
    await ingresar(page, AUTORIDAD)
    await expect(page.getByRole('heading', { name: 'Panel de autoridad' })).toBeVisible()
    await page.goto(`/reportes/${id}`)
    await page.getByLabel('Nuevo estado').selectOption('EN_REVISION')
    await page.getByLabel('Nota para el ciudadano (opcional)').fill('Patrulla enviada (prueba e2e)')
    await page.getByRole('button', { name: /Guardar|Actualizar|Cambiar/ }).click()
    await expect(page.getByText('En revisión').first()).toBeVisible()
    const bandeja = await (await api(request, luis, 'GET', '/notificaciones')).json()
    expect(bandeja.notificaciones[0].tipo).toBe('ESTADO_REPORTE')
  })

  test('UI-22 exporta el CSV de la semana', async ({ page }) => {
    await ingresar(page, AUTORIDAD)
    const descarga = page.waitForEvent('download')
    await page.getByRole('button', { name: /Exportar semana/ }).click()
    expect((await descarga).suggestedFilename()).toMatch(/\.csv$/)
  })
})
