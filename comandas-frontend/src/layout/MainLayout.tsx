import { NavLink, Outlet } from 'react-router-dom';
import { LayoutGrid, DollarSign, Coffee } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';

/**
 * Ítem de navegación del dashboard
 */
interface NavItem {
  to: string;
  icon: LucideIcon;
  label: string;
}

const navItems: NavItem[] = [
  { to: '/',          icon: LayoutGrid, label: 'Salón' },
  { to: '/caja',      icon: DollarSign, label: 'Caja' },
  { to: '/mostrador', icon: Coffee,     label: 'Mostrador' },
];

/**
 * Layout principal (Dashboard Layout)
 *
 * Envuelve todas las rutas de la aplicación con una Navbar
 * superior fija. Usa <Outlet /> de react-router-dom para
 * renderizar la ruta activa como contenido.
 *
 * Decisión: Navbar horizontal superior (no sidebar) porque
 * el sistema corre en pantallas pequeñas/tabletas y necesitamos
 * maximizar el área útil vertical para el grid de mesas.
 */
export default function MainLayout() {
  return (
    <div className="min-h-screen bg-neutral-900 flex flex-col">
      {/* ── Navbar Superior ── */}
      <header className="sticky top-0 z-50 bg-neutral-900 border-b border-gray-800">
        <nav className="max-w-7xl mx-auto h-16 px-4 flex items-center justify-between">
          {/* Logo / Marca */}
          <span className="text-xl font-bold text-red-500 tracking-tight select-none">
            FoodFlow
          </span>

          {/* Links de navegación */}
          <ul className="flex items-center gap-1">
            {navItems.map(({ to, icon: Icon, label }) => (
              <li key={to}>
                <NavLink
                  to={to}
                  end={to === '/'}
                  className={({ isActive }) =>
                    [
                      'relative flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors',
                      isActive
                        ? 'text-red-500 bg-red-500/10'
                        : 'text-gray-400 hover:text-gray-100 hover:bg-neutral-800',
                    ].join(' ')
                  }
                >
                  {({ isActive }) => (
                    <>
                      <Icon size={18} strokeWidth={isActive ? 2.5 : 2} />
                      <span>{label}</span>
                    </>
                  )}
                </NavLink>
              </li>
            ))}
          </ul>

          {/* Espacio reservado para futuras acciones (ej: usuario, config) */}
          <div className="w-20" />
        </nav>
      </header>

      {/* ── Contenido de la ruta activa ── */}
      <main className="flex-1">
        <Outlet />
      </main>
    </div>
  );
}
