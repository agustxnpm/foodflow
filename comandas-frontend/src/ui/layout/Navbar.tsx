import { Link } from 'react-router-dom';
import { Home, Users, ShoppingCart, DollarSign, Package, Tag } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';

interface NavItem {
  to: string;
  icon: LucideIcon;
  label: string;
}

export default function Navbar() {
  const navItems: NavItem[] = [
    { to: '/', icon: Home, label: 'Inicio' },
    { to: '/salon', icon: Users, label: 'Salón' },
    { to: '/pedido', icon: ShoppingCart, label: 'Pedido' },
    { to: '/caja', icon: DollarSign, label: 'Caja' },
    { to: '/catalogo', icon: Package, label: 'Catálogo' },
    { to: '/promociones', icon: Tag, label: 'Promos' },
  ];

  return (
    <nav className="bg-background-card border-b border-gray-800">
      <div className="px-4 h-16 flex items-center gap-6">
        <div className="font-bold text-xl text-primary">FoodFlow</div>
        <div className="flex gap-4">
          {navItems.map(({ to, icon: Icon, label }) => (
            <Link
              key={to}
              to={to}
              className="flex items-center gap-2 px-4 py-2 rounded hover:bg-gray-700 transition-colors"
            >
              <Icon size={20} />
              <span className="text-sm">{label}</span>
            </Link>
          ))}
        </div>
      </div>
    </nav>
  );
}
