import type { Mesa } from '../types';
import MesaCard from './MesaCard';

interface MesaGridProps {
  mesas: Mesa[];
  onMesaClick: (mesa: Mesa) => void;
  modoEdicion?: boolean;
  onEliminar?: (mesaId: string) => void;
  isLoading?: boolean;
  isError?: boolean;
}

/**
 * Grid responsivo que distribuye las mesas del salón.
 *
 * Componente puramente presentacional: recibe las mesas y estados de carga
 * como props, delegando el data-fetching al padre (SalonPage vía useSalonState).
 *
 * Layout: 7 columnas en desktop (simula la distribución física del local),
 * 4 en tablet, 3 en mobile.
 *
 * HU-02: Ver estado de mesas
 */
export default function MesaGrid({
  mesas,
  onMesaClick,
  modoEdicion = false,
  onEliminar,
  isLoading = false,
  isError = false,
}: MesaGridProps) {
  // Skeleton loading: muestra placeholders pulsantes mientras carga
  if (isLoading) {
    return (
      <div className="grid grid-cols-4 sm:grid-cols-5 lg:grid-cols-7 gap-3">
        {Array.from({ length: 14 }).map((_, i) => (
          <div
            key={i}
            className="
              aspect-square rounded-xl
              bg-neutral-900 border-2 border-neutral-800
              animate-pulse
              flex flex-col items-center justify-center gap-1.5
            "
          >
            <div className="w-10 h-10 rounded-lg bg-neutral-800" />
            <div className="w-12 h-3 rounded bg-neutral-800" />
            <div className="w-10 h-3 rounded-full bg-neutral-800" />
          </div>
        ))}
      </div>
    );
  }

  // Error state
  if (isError) {
    return (
      <div
        className="
          flex items-center justify-center
          min-h-[400px]
          bg-neutral-800/50
          border-2 border-red-900/50
          rounded-2xl
        "
      >
        <div className="text-center space-y-2">
          <p className="text-red-500 text-lg font-semibold">
            Error al cargar mesas
          </p>
          <p className="text-gray-500 text-sm">
            Verificá la conexión con el backend
          </p>
        </div>
      </div>
    );
  }

  // Empty state
  if (mesas.length === 0) {
    return (
      <div
        className="
          flex items-center justify-center
          min-h-[400px]
          bg-neutral-800/50
          border-2 border-dashed border-neutral-700
          rounded-2xl
        "
      >
        <p className="text-gray-500 text-lg">
          No hay mesas en el salón. Creá la primera.
        </p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-4 sm:grid-cols-5 lg:grid-cols-7 gap-3">
      {mesas.map((mesa) => (
        <MesaCard
          key={mesa.id}
          mesa={mesa}
          onClick={onMesaClick}
          modoEdicion={modoEdicion}
          onEliminar={onEliminar}
        />
      ))}
    </div>
  );
}
