import { expect, test } from '@playwright/test'
import { api, ingresar, token, totp } from './ayudas'

test.describe('HU 10 Configuración y seguridad', () => {
  const CORREO = 'vecino5@alertapp.co'

  test('UI-16 activa la verificación en dos pasos, ingresa con código y la desactiva', async ({ page }) => {
    await ingresar(page, CORREO)
    await page.goto('/perfil/seguridad')
    await page.getByRole('button', { name: 'Activar' }).click()
    await expect(page.getByAltText('Código QR para la app autenticadora')).toBeVisible()
    await page.getByText('No puedo escanear').click()
    const secreto = (await page.locator('code.secreto').innerText()).replace(/\s/g, '')
    await page.getByLabel('Código de 6 dígitos de la app').fill(totp(secreto))
    await page.getByRole('button', { name: 'Confirmar y activar' }).click()
    await expect(page.locator('#t-2fa').locator('..')).toContainText('Activa')

    // Salir y volver a entrar: ahora pide el código.
    await page.goto('/perfil')
    await page.getByRole('button', { name: 'Salir' }).click()
    await page.getByLabel('Correo').fill(CORREO)
    await page.getByLabel('Contraseña').fill('Valledupar2026')
    await page.getByRole('button', { name: 'Ingresar' }).click()
    await expect(page.getByRole('heading', { name: 'Verificación en dos pasos' })).toBeVisible()
    await page.getByLabel('Código').fill(totp(secreto))
    await page.getByRole('button', { name: 'Verificar' }).click()
    await expect(page).not.toHaveURL(/\/ingreso/)

    await page.goto('/perfil/seguridad')
    await page.locator('#s-clave-2fa').fill('Valledupar2026')
    await page.getByLabel('Código de 6 dígitos de la app').fill(totp(secreto))
    await page.getByRole('button', { name: 'Desactivar' }).click()
    await expect(page.locator('#t-2fa').locator('..')).toContainText('Inactiva')
  })

  test('UI-17 desactiva y vuelve a activar las alertas cercanas', async ({ page }) => {
    await ingresar(page, CORREO)
    await page.goto('/perfil/seguridad')
    const interruptor = page.getByRole('switch')
    await expect(interruptor).toBeChecked()
    await interruptor.uncheck()
    await expect(page.locator('.aviso')).toContainText('Alertas desactivadas')
    await interruptor.check()
    await expect(page.locator('.aviso')).toContainText('Alertas activas')
  })

  test('UI-18 cambia la contraseña con la actual y la restaura', async ({ page, request }) => {
    await ingresar(page, CORREO)
    await page.goto('/perfil/seguridad')
    await page.getByLabel('Contraseña actual').fill('Valledupar2026')
    await page.getByLabel('Contraseña nueva').fill('Temporal2026x')
    await page.getByRole('button', { name: 'Cambiar contraseña' }).click()
    await expect(page.locator('.aviso')).toContainText('Contraseña actualizada')
    const tk = await token(request, CORREO, 'Temporal2026x')
    const r = await api(request, tk, 'PUT', '/auth/yo/clave', { actual: 'Temporal2026x', nueva: 'Valledupar2026' })
    expect(r.status()).toBe(204)
  })
})
