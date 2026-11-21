import { NavLink, Outlet } from 'react-router-dom'
import {
  BellSimple,
  ChartBar,
  MapTrifold,
  NavigationArrow,
  PlusCircle,
  ShieldCheck,
  UserCircle,
  UsersThree,
} from '@phosphor-icons/react'
import { useSesion } from '../sesion'
import { useTiempoReal } from '../tiempoReal'
import { ProveedorSeguimiento } from '../seguimiento'
import Logo from './Logo'

export default function Shell() {
  const { usuario } = useSesion()
  const { bandeja } = useTiempoReal()
  const esAutoridad = usuario?.rol === 'AUTORIDAD'
  const sinLeer = bandeja.sinLeer
  return (
    <ProveedorSeguimiento>
      <div className="app">
        <main>
          <Outlet />
        </main>
        <nav className="nav" aria-label="Navegación principal">
          <span className="marca">
            <Logo tam={28} /> AlertApp
          </span>
          <NavLink to="/" end>
            <MapTrifold size={24} weight="bold" aria-hidden /> Mapa
          </NavLink>
          {esAutoridad ? (
            <NavLink to="/panel">
              <ShieldCheck size={24} weight="bold" aria-hidden /> Panel
            </NavLink>
          ) : (
            <>
              <NavLink to="/rutas">
                <NavigationArrow size={24} weight="bold" aria-hidden /> Rutas
              </NavLink>
              <NavLink to="/reportar">
                <PlusCircle size={24} weight="bold" aria-hidden /> Reportar
              </NavLink>
            </>
          )}
          <NavLink to="/comunidad">
            <UsersThree size={24} weight="bold" aria-hidden /> Comunidad
          </NavLink>
          {/* En el celular del ciudadano Métricas se abre desde el perfil para dejar 6 botones legibles. */}
          <NavLink to="/metricas" className={esAutoridad ? '' : 'solo-escritorio'}>
            <ChartBar size={24} weight="bold" aria-hidden /> Métricas
          </NavLink>
          <NavLink to="/avisos" aria-label={sinLeer > 0 ? `Avisos, ${sinLeer} sin leer` : 'Avisos'}>
            <span className="icono-badge">
              <BellSimple size={24} weight="bold" aria-hidden />
              {sinLeer > 0 && (
                <span className="badge num" aria-hidden>
                  {sinLeer > 9 ? '9+' : sinLeer}
                </span>
              )}
            </span>
            Avisos
          </NavLink>
          <NavLink to="/perfil">
            <UserCircle size={24} weight="bold" aria-hidden /> Perfil
          </NavLink>
        </nav>
      </div>
    </ProveedorSeguimiento>
  )
}
