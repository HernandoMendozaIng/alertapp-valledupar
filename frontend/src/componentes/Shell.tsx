import { NavLink, Outlet } from 'react-router-dom'
import { ChartBar, MapTrifold, PlusCircle, ShieldCheck, UserCircle, UsersThree } from '@phosphor-icons/react'
import { useSesion } from '../sesion'
import Logo from './Logo'

export default function Shell() {
  const { usuario } = useSesion()
  const esAutoridad = usuario?.rol === 'AUTORIDAD'
  return (
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
          <NavLink to="/reportar">
            <PlusCircle size={24} weight="bold" aria-hidden /> Reportar
          </NavLink>
        )}
        <NavLink to="/comunidad">
          <UsersThree size={24} weight="bold" aria-hidden /> Comunidad
        </NavLink>
        <NavLink to="/metricas">
          <ChartBar size={24} weight="bold" aria-hidden /> Métricas
        </NavLink>
        <NavLink to="/perfil">
          <UserCircle size={24} weight="bold" aria-hidden /> Perfil
        </NavLink>
      </nav>
    </div>
  )
}
