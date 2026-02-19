import { Tag } from 'lucide-react';

/**
 * Colores disponibles para categorizar productos en el catálogo.
 * Cada color agrupa productos visualmente en la grilla del POS.
 *
 * El backend devuelve `colorHex` por producto. Aquí definimos el mapping
 * color → label para la navegación lateral.
 *
 * DECISIÓN: Se usa `null` como categoría "Todos" para no filtrar.
 */
export interface CategoriaColor {
  /** null = sin filtro (todos los productos) */
  color: string | null;
  label: string;
  /** Hex para renderizar el indicador visual */
  hex: string;
}

/**
 * Categorías por color del local.
 * Esto debería eventualmente venir del backend como configuración del tenant.
 * Por ahora se define estáticamente alineado al seed de datos.
 */
export const CATEGORIAS: CategoriaColor[] = [
  { color: null, label: 'Todos', hex: '#FFFFFF' },
  { color: '#FF0000', label: 'Hamburguesas', hex: '#FF0000' },
  { color: '#0000FF', label: 'Bebidas', hex: '#3B82F6' },
  { color: '#00FF00', label: 'Ensaladas', hex: '#22C55E' },
  { color: '#FFA500', label: 'Postres', hex: '#F97316' },
  { color: '#800080', label: 'Pizzas', hex: '#A855F7' },
  { color: '#FFFF00', label: 'Extras', hex: '#EAB308' },
];

interface ListaCategoriasProps {
  categoriaActiva: string | null;
  onCategoriaChange: (color: string | null) => void;
}

/**
 * Panel lateral izquierdo del POS: navegación por categorías de productos.
 *
 * Cada botón filtra la grilla central por `colorHex`.
 * El botón activo se resalta con borde rojo y fondo oscuro.
 *
 * Touch-friendly: botones de altura mínima 48px.
 */
export default function ListaCategorias({
  categoriaActiva,
  onCategoriaChange,
}: ListaCategoriasProps) {
  return (
    <nav className="flex flex-col gap-1.5 py-4 px-3" aria-label="Categorías de productos">
      {CATEGORIAS.map((cat) => {
        const isActiva = categoriaActiva === cat.color;

        return (
          <button
            key={cat.label}
            type="button"
            onClick={() => onCategoriaChange(cat.color)}
            className={[
              'flex items-center gap-3',
              'w-full px-3 py-3 rounded-xl',
              'text-sm font-semibold text-left',
              'transition-all duration-150',
              'active:scale-[0.97]',
              isActiva
                ? 'bg-neutral-800 text-gray-100 border border-red-600/50 shadow-sm'
                : 'text-gray-500 hover:bg-neutral-800/50 hover:text-gray-300 border border-transparent',
            ].join(' ')}
            aria-current={isActiva ? 'page' : undefined}
          >
            {/* Indicador de color */}
            {cat.color ? (
              <span
                className="w-3.5 h-3.5 rounded-full shrink-0 border border-white/10"
                style={{ backgroundColor: cat.hex }}
              />
            ) : (
              <Tag size={14} className="shrink-0 text-gray-500" />
            )}

            <span className="truncate">{cat.label}</span>
          </button>
        );
      })}
    </nav>
  );
}
