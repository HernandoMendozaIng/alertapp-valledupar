import { defineConfig, devices } from '@playwright/test'

/**
 * Pruebas de extremo a extremo de la interfaz (TRL 5). Requieren los tres servicios corriendo con los datos
 * de demostración (./scripts/iniciar.sh). Se ejecutan en un celular simulado, que es el uso principal de la app.
 */
export default defineConfig({
  testDir: './e2e',
  // Las capturas para la documentación solo se generan con npm run capturas.
  testIgnore: process.env.CAPTURAS ? [] : ['capturas/**'],
  timeout: 45_000,
  expect: { timeout: 8_000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list'], ['html', { outputFolder: 'e2e-reporte', open: 'never' }]],
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:5173',
    ...devices['Pixel 7'],
    // Usa el Google Chrome instalado; en CI se puede cambiar con PW_CHANNEL=chromium.
    channel: process.env.PW_CHANNEL ?? 'chrome',
    locale: 'es-CO',
    timezoneId: 'America/Bogota',
    // Primero de Mayo, Comuna 3
    geolocation: { latitude: 10.4541, longitude: -73.2529 },
    permissions: ['geolocation'],
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
})
