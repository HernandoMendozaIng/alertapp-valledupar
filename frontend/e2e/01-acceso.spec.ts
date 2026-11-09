import { expect, test } from '@playwright/test'
import { CIUDADANO, ingresar } from './ayudas'

test.describe('HU 01 Acceso y autenticación', () => {
  test('UI-01 ingresa con la cuenta de prueba y sale', async ({ page }) => {
    await ingresar(page, CIUDADANO)
    await expect(page.getByRole('heading', { name: /Hola, Luis/ })).toBeVisible()
    await page.getByRole('link', { name: 'Perfil' }).click()
    await page.getByRole('button', { name: 'Salir' }).click()
    await expect(page).toHaveURL(/\/ingreso/)
  })

  test('UI-02 muestra el error con una contraseña equivocada', async ({ page }) => {
    await page.goto('/ingreso')
    await page.getByLabel('Correo').fill(CIUDADANO)
    await page.getByLabel('Contraseña').fill('Equivocada1')
    await page.getByRole('button', { name: 'Ingresar' }).click()
    await expect(page.getByRole('alert')).toHaveText('Correo o contraseña incorrectos')
  })

  test('UI-03 recuperar contraseña pide el código después del correo', async ({ page }) => {
    await page.goto('/ingreso')
    await page.getByRole('link', { name: '¿Olvidaste tu contraseña?' }).click()
    await page.getByLabel('Correo').fill('nadie.registrado@prueba.co')
    await page.getByRole('button', { name: 'Enviar código' }).click()
    // Responde igual exista o no la cuenta.
    await expect(page.getByLabel('Código')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Restablecer contraseña' })).toBeDisabled()
  })

  test('UI-04 el registro valida los datos antes de crear la cuenta', async ({ page }) => {
    await page.goto('/registro')
    await page.getByRole('button', { name: /Crear cuenta/ }).click()
    await expect(page.locator('.error').first()).toBeVisible()
  })
})
