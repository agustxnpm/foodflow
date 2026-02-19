import type { Mesa } from '../types';
import { X } from 'lucide-react';

interface MesaCardProps {
  mesa: Mesa;
  onClick: (mesa: Mesa) => void;
  onEliminar?: (mesaId: string) => void;
  modoEdicion?: boolean;
}

/**
 * Representación visual de una mesa física del salón.
 *
 * Usa el ícono mesa-de-comedor.png con el número superpuesto.
 * Diferenciación visual por estado (Lenguaje Ubicuo):
 * - LIBRE: gris/opaco → disponible para comensales
 * - ABIERTA: acento rojo, borde iluminado → pedido activo
 *
 * La info detallada del pedido (hora, total) vive en el SidebarResumen.
 *
 * HU-02: Visualización de estado de mesas
 */
export default function MesaCard({
  mesa,
  onClick,
  onEliminar,
  modoEdicion = false,
}: MesaCardProps) {
  const isAbierta = mesa.estado === 'ABIERTA';
  const isLibre = mesa.estado === 'LIBRE';

  const handleEliminar = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (onEliminar && isLibre) onEliminar(mesa.id);
  };

  return (
    <button
      type="button"
      onClick={() => onClick(mesa)}
      className={[
        'group relative flex flex-col items-center justify-center gap-1',
        'w-full aspect-square rounded-xl p-2',
        'transition-all duration-200 cursor-pointer',
        'active:scale-95 focus:outline-none focus-visible:ring-2 focus-visible:ring-red-500',
        isAbierta
          ? 'bg-gradient-to-b from-red-950/60 to-neutral-900 border-2 border-red-600 shadow-md shadow-red-950/30 hover:border-red-400'
          : 'bg-neutral-900 border-2 border-neutral-800 hover:border-neutral-600 hover:bg-neutral-800/80',
      ].join(' ')}
      aria-label={`Mesa ${mesa.numero} — ${mesa.estado}`}
    >
      {/* Botón eliminar (modo edición, solo mesa libre) */}
      {modoEdicion && isLibre && onEliminar && (
        <span
          role="button"
          tabIndex={0}
          onClick={handleEliminar}
          onKeyDown={(e) =>
            e.key === 'Enter' && handleEliminar(e as unknown as React.MouseEvent)
          }
          className="
            absolute -top-1.5 -right-1.5 z-10
            w-6 h-6 rounded-full
            bg-red-600 text-white
            flex items-center justify-center
            hover:bg-red-500 transition-colors
            shadow-md
          "
          aria-label={`Eliminar mesa ${mesa.numero}`}
        >
          <X size={12} strokeWidth={3} />
        </span>
      )}

      {/* Ícono PNG de mesa con número superpuesto */}
      <div className="relative w-12 h-12 sm:w-14 sm:h-14">
        <div
          className={[
            'absolute inset-0 rounded-lg',
            isAbierta ? 'bg-red-900/40' : 'bg-neutral-700/20',
          ].join(' ')}
        />
        <img
          src="/mesa-de-comedor.png"
          alt=""
          aria-hidden="true"
          className={[
            'absolute inset-0.5 w-[calc(100%-0.25rem)] h-[calc(100%-0.25rem)] object-contain',
            isLibre ? 'opacity-25 grayscale' : 'opacity-75',
          ].join(' ')}
          draggable={false}
        />
        <span
          className={[
            'relative z-10 flex items-center justify-center w-full h-full',
            'text-2xl sm:text-3xl font-extrabold drop-shadow-lg tabular-nums',
            isAbierta ? 'text-white' : 'text-gray-400',
          ].join(' ')}
        >
          {mesa.numero}
        </span>
      </div>

      {/* Label "Mesa N" */}
      <span
        className={[
          'text-sm font-bold leading-tight',
          isAbierta ? 'text-gray-200' : 'text-neutral-500',
        ].join(' ')}
      >
        Mesa {mesa.numero}
      </span>

      {/* Badge de estado */}
      <span
        className={[
          'text-[10px] font-bold uppercase tracking-[0.12em] px-2 py-0.5 rounded-full',
          isAbierta
            ? 'bg-red-600/20 text-red-400 border border-red-600/30'
            : 'bg-neutral-800 text-neutral-600 border border-neutral-700',
        ].join(' ')}
      >
        {isAbierta ? 'Ocupada' : 'Libre'}
      </span>
    </button>
  );
}
