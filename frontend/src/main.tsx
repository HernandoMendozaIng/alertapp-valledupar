import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import '@fontsource/roboto/400.css'
import '@fontsource/roboto/500.css'
import '@fontsource/roboto/700.css'
import 'leaflet/dist/leaflet.css'
import './index.css'
import App from './App'
import { ProveedorSesion } from './sesion'
import { ProveedorAviso } from './aviso'
import { ProveedorTiempoReal } from './tiempoReal'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <ProveedorSesion>
        <ProveedorAviso>
          <ProveedorTiempoReal>
            <App />
          </ProveedorTiempoReal>
        </ProveedorAviso>
      </ProveedorSesion>
    </BrowserRouter>
  </StrictMode>,
)
