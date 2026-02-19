import type { Mesa } from '../types';
import { Clock } from 'lucide-react';

interface MesaCardProps {
  mesa: Mesa;
  onClick: (mesa: Mesa) => void;
  onEliminar?: (mesaId: string) => void;
  modoEdicion?: boolean;
}

/**
 * Representación visual de una mesa física del salón.
 *
 * Usa el icono mesa-de-comedor.png como representación geométrica
 * con diferenciación visual por estado (Lenguaje Ubicuo):
 * - LIBRE: gris/opaco → mesa disponible para nuevos comensales
 * - ABIERTA: acento rojo → mesa con pedido activo
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

  const formatHora = (fecha: string) => {
    const d = new Date(fecha);
    return d.toLocaleTimeString('es-AR', {
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const handleEliminar = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (onEliminar && isLibre) onEliminar(mesa.id);
  };

  return (
    <button
      type="button"
      onClick={() => onClick(mesa)}
      className={[
        'relative flex flex-col items-center justify-center',
        'w-full aspect-square rounded-2xl p-3',
        'transition-all duration-200 cursor-pointer',
        'active:scale-95 focus:outline-none focus-visible:ring-2 focus-visible:ring-red-500',
        isAbierta
          ? 'bg-red-950/50 border-2 border-red-600 shadow-lg shadow-red-950/40 hover:border-red-400'
          : 'bg-neutral-800/70 border-2 border-neutral-700 hover:border-neutral-500 hover:bg-neutral-800',
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
            absolute -top-2 -right-2 z-10
            w-7 h-7 rounded-full
            bg-red-600 text-white text-sm font-bold
            flex items-center justify-center
            hover:bg-red-500 transition-colors
          "
          aria-label={`Eliminar mesa ${mesa.numero}`}
        >
          ×
        </span>
      )}

      {/* Icono de mesa con número superpuesto */}
      <div className="relative w-16 h-16 sm:w-20 sm:h-20 mb-1">
        {/* Fondo de contraste para el icono PNG transparente */}
        <div
          className={[
            'absolute inset-0 rounded-xl',
            isAbierta ? 'bg-red-900/40' : 'bg-neutral-700/30',
          ].join(' ')}
        />
        <img
          src="/mesa-de-comedor.png"
          alt=""
          aria-hidden="true"
          className={[
            'absolute inset-1 w-[calc(100%-0.5rem)] h-[calc(100%-0.5rem)] object-contain',
            isLibre ? 'opacity-30 grayscale' : 'opacity-80',
          ].join(' ')}
          draggable={false}
        />
        {/* Número de mesa superpuesto */}
        <span
          className={[
            'relative z-10 flex items-center justify-center w-full h-full',
            'text-2xl sm:text-3xl font-extrabold drop-shadow-lg',
            isAbierta ? 'text-white' : 'text-gray-400',
          ].join(' ')}
        >
          {mesa.numero}
        </span>
      </div>

      {/* Estado: LIBRE */}
      {isLibre && (
        <span className="text-[11px] text-gray-500 uppercase tracking-widest font-semibold mt-1">
          Libre
        </span>
      )}

      {/* Estado: ABIERTA — info rápida */}
      {isAbierta && mesa.pedidoActual && (
        <div className="flex flex-col items-center gap-0.5 mt-1">
          <div className="flex items-center gap-1 text-[11px] text-gray-400">
            <Clock size={11} />
            <span>{formatHora(mesa.pedidoActual.fechaApertura)}</span>
          </div>
          <span className="text-sm font-bold text-red-400 font-mono">
            $ {mesa.pedidoActual.total.toLocaleString('es-AR')}
          </span>
        </div>
      )}
    </button>
  );
}
