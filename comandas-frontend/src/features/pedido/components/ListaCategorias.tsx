import { Tag } from 'lucide-react';
import { useCategoriasUI } from '../../../lib/categorias-ui';

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
 * Las categorías se cargan desde Tauri Store (persistentes y editables).
 *
 * Touch-friendly: botones de altura mínima 48px.
 */
export default function ListaCategorias({
  categoriaActiva,
  onCategoriaChange,
}: ListaCategoriasProps) {
  const { categoriasConTodos } = useCategoriasUI();

  return (
    <nav className="flex flex-col gap-1.5 py-4 px-3" aria-label="Categorías de productos">
      {categoriasConTodos.map((cat) => {
        // "Todos" tiene colorBase vacío → equivale a null
        const colorFiltro = cat.colorBase || null;
        const isActiva = categoriaActiva === colorFiltro;

        return (
          <button
            key={cat.id}
            type="button"
            onClick={() => onCategoriaChange(colorFiltro)}
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
            {cat.colorBase ? (
              <span
                className="w-3.5 h-3.5 rounded-full shrink-0 border border-white/10"
                style={{ backgroundColor: cat.colorDisplay }}
              />
            ) : (
              <Tag size={14} className="shrink-0 text-gray-500" />
            )}

            <span className="truncate">{cat.nombre}</span>
          </button>
        );
      })}
    </nav>
  );
}
