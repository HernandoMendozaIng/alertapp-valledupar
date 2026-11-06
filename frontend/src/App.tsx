import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useSesion } from './sesion'
import Shell from './componentes/Shell'
import Ingreso from './paginas/Ingreso'
import Registro from './paginas/Registro'
import Mapa from './paginas/Mapa'
import Reportar from './paginas/Reportar'
import Comunidad from './paginas/Comunidad'
import DetalleReporte from './paginas/DetalleReporte'
import Metricas from './paginas/Metricas'
import Perfil from './paginas/Perfil'
import Panel from './paginas/Panel'
import Informe from './paginas/Informe'

function Protegida({ children, soloAutoridad = false }: { children: React.ReactNode; soloAutoridad?: boolean }) {
  const { usuario, cargando } = useSesion()
  const ubicacion = useLocation()
  if (cargando) return <div className="pagina" aria-busy="true" />
  if (!usuario) return <Navigate to="/ingreso" replace state={{ desde: ubicacion.pathname }} />
  if (soloAutoridad && usuario.rol !== 'AUTORIDAD') return <Navigate to="/" replace />
  return <>{children}</>
}

export default function App() {
  const { usuario } = useSesion()
  return (
    <Routes>
      <Route path="/ingreso" element={usuario ? <Navigate to="/" replace /> : <Ingreso />} />
      <Route path="/registro" element={usuario ? <Navigate to="/" replace /> : <Registro />} />
      <Route
        element={
          <Protegida>
            <Shell />
          </Protegida>
        }
      >
        <Route index element={<Mapa />} />
        <Route path="reportar" element={<Reportar />} />
        <Route path="reportes/:id/editar" element={<Reportar />} />
        <Route path="reportes/:id" element={<DetalleReporte />} />
        <Route path="comunidad" element={<Comunidad />} />
        <Route path="metricas" element={<Metricas />} />
        <Route path="perfil" element={<Perfil />} />
        <Route
          path="panel"
          element={
            <Protegida soloAutoridad>
              <Panel />
            </Protegida>
          }
        />
        <Route
          path="panel/informe"
          element={
            <Protegida soloAutoridad>
              <Informe />
            </Protegida>
          }
        />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
