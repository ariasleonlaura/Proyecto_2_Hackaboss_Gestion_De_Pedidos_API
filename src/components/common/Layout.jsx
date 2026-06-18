import { Outlet, NavLink } from 'react-router-dom';

const Layout = () => {
  return (
    <div className="layout-container">
     
      <header className="navbar">
        <h1>Gestión de Pedidos</h1>
        <nav>
          <ul>
            <li>
              <NavLink to="/">Terminal</NavLink>
            </li>
            <li>
              <NavLink to="/cocina">Cocina</NavLink>
            </li>
            <li>
              <NavLink to="/recogida">Recogida</NavLink>
            </li>
          </ul>
        </nav>
      </header>

      
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
};

export default Layout;