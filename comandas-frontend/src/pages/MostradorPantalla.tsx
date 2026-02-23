import { useState } from 'react';
import { Package, Sparkles } from 'lucide-react';
import VistaCatalogo from '../features/catalogo/components/VistaCatalogo';
import VistaPromociones from '../features/promociones/components/VistaPromociones';

type TabActiva = 'catalogo' | 'promociones';

interface TabConfig {
  id: TabActiva;
  label: string;
  icon: typeof Package;
}

const tabs: TabConfig[] = [
  { id: 'catalogo', label: 'Catálogo y Stock', icon: Package },
  { id: 'promociones', label: 'Motor de Promociones', icon: Sparkles },
];

/**
 * Pantalla principal del módulo Mostrador / Admin.
 *
 * Organiza la gestión del local en dos pestañas:
 * 1. Catálogo y Stock → productos, precios, inventario
 * 2. Motor de Promociones → reglas, estrategias, alcances
 *
 * Decisión: Tabs horizontales (no rutas anidadas) porque el operador
 * cambia frecuentemente entre secciones y queremos preservar estado
 * de búsqueda/filtros sin perderlo al cambiar de tab.
 */
export default function MostradorPantalla() {
  const [tabActiva, setTabActiva] = useState<TabActiva>('catalogo');

  return (
    <div className="min-h-full flex flex-col">
      {/* ── Barra de Tabs ── */}
      <div className="sticky top-16 z-40 bg-neutral-900 border-b border-gray-800">
        <div className="max-w-7xl mx-auto px-4">
          <nav className="flex gap-1" role="tablist">
            {tabs.map(({ id, label, icon: Icon }) => (
              <button
                key={id}
                role="tab"
                aria-selected={tabActiva === id}
                onClick={() => setTabActiva(id)}
                className={[
                  'flex items-center gap-2 px-5 py-3 text-sm font-medium border-b-2 transition-colors',
                  tabActiva === id
                    ? 'text-red-500 border-red-500'
                    : 'text-gray-400 border-transparent hover:text-gray-200 hover:border-gray-600',
                ].join(' ')}
              >
                <Icon size={18} />
                <span>{label}</span>
              </button>
            ))}
          </nav>
        </div>
      </div>

      {/* ── Contenido de la Tab Activa ── */}
      <div className="flex-1 max-w-7xl mx-auto w-full px-4 py-6">
        {tabActiva === 'catalogo' && <VistaCatalogo />}
        {tabActiva === 'promociones' && <VistaPromociones />}
      </div>
    </div>
  );
}
