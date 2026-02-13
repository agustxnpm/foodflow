import { Outlet, Link } from 'react-router-dom';
import './MainLayout.css';

export function MainLayout() {
  return (
    <div className="main-layout">
      <aside className="sidebar">
        <h2>FoodFlow</h2>
        <nav>
          <ul>
            <li>
              <Link to="/">Dashboard</Link>
            </li>
            <li>
              <Link to="/mesas">Mesas</Link>
            </li>
            <li>
              <Link to="/productos">Productos</Link>
            </li>
          </ul>
        </nav>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
