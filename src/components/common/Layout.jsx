import { NavLink, Outlet } from "react-router-dom";


function Layout() {
  return (
    <div className="layout">
      <nav className="navbar">
        <span className="navbar__titulo">Gestión de Pedidos</span>
        <div className="navbar__links">
          <NavLink
            to="/terminal"
            className={({ isActive }) =>
              isActive ? "navbar__link navbar__link--active" : "navbar__link"
            }
          >
            Terminal
          </NavLink>
          <NavLink
            to="/cocina"
            className={({ isActive }) =>
              isActive ? "navbar__link navbar__link--active" : "navbar__link"
            }
          >
            Cocina
          </NavLink>
          <NavLink
            to="/recogida"
            className={({ isActive }) =>
              isActive ? "navbar__link navbar__link--active" : "navbar__link"
            }
          >
            Recogida
          </NavLink>
        </div>
      </nav>

      <main className="layout__contenido">
        <Outlet />
      </main>
    </div>
  );
}


export default Layout;