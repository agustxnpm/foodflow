import { Tag, Loader2 } from 'lucide-react';
import { useCategorias } from '../../categorias/hooks/useCategorias';

interface ListaCategoriasProps {
  categoriaActiva: string | null;
  onCategoriaChange: (categoriaId: string | null) => void;
}

/**
 * Panel lateral izquierdo del POS: navegación por categorías de productos.
 *
 * Cada botón filtra la grilla central por `categoriaId` (UUID del backend).
 * El botón activo se resalta con borde rojo y fondo oscuro.
 *
 * Las categorías se cargan desde el backend (entidades de dominio).
 * Se excluyen las categorías marcadas como `esCategoriaExtra`
 * ya que los extras no se muestran como categorías navegables en el POS.
 *
 * Touch-friendly: botones de altura mínima 48px.
 */
export default function ListaCategorias({
  categoriaActiva,
  onCategoriaChange,
}: ListaCategoriasProps) {
  const { data: categorias = [], isLoading } = useCategorias();

  // Filtrar categorías extras: no se navegan en el POS
  const categoriasNavegables = categorias.filter((c) => !c.esCategoriaExtra);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8">
        <Loader2 size={18} className="text-gray-600 animate-spin" />
      </div>
    );
  }

  return (
    <nav className="flex flex-col gap-1.5 py-4 px-3" aria-label="Categorías de productos">
      {/* Botón "Todos" — sin filtro */}
      <button
        type="button"
        onClick={() => onCategoriaChange(null)}
        className={[
          'flex items-center gap-3',
          'w-full px-3 py-3 rounded-xl',
          'text-sm font-semibold text-left',
          'transition-all duration-150',
          'active:scale-[0.97]',
          categoriaActiva === null
            ? 'bg-neutral-800 text-gray-100 border border-red-600/50 shadow-sm'
            : 'text-gray-500 hover:bg-neutral-800/50 hover:text-gray-300 border border-transparent',
        ].join(' ')}
        aria-current={categoriaActiva === null ? 'page' : undefined}
      >
        <Tag size={14} className="shrink-0 text-gray-500" />
        <span className="truncate">Todos</span>
      </button>

      {/* Categorías del backend */}
      {categoriasNavegables.map((cat) => {
        const isActiva = categoriaActiva === cat.id;

        return (
          <button
            key={cat.id}
            type="button"
            onClick={() => onCategoriaChange(cat.id)}
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
            <span
              className="w-3.5 h-3.5 rounded-full shrink-0 border border-white/10"
              style={{ backgroundColor: cat.colorHex }}
            />
            <span className="truncate">{cat.nombre}</span>
          </button>
        );
      })}
    </nav>
  );
}
