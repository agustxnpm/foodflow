import { RotateCcw, ShoppingBag } from 'lucide-react';
import type { VentaResumen } from '../types';

// ─── Utilidad de formato ──────────────────────────────────────────────────────

function formatMonto(valor: number): string {
  return valor.toLocaleString('es-AR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function formatHora(isoFecha: string): string {
  const d = new Date(isoFecha);
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

// ─── Componente principal ─────────────────────────────────────────────────────

interface HistorialVentasProps {
  ventas: VentaResumen[];
  isLoading: boolean;
  /** Callback para abrir el modal de corrección (recibe pedidoId) */
  onCorregirPedido: (pedidoId: string) => void;
}

/**
 * Historial de pedidos cerrados del día (ventas).
 *
 * Cada fila muestra:
 * - Hora de cierre
 * - Mesa + Nº de pedido
 * - Monto total cobrado
 * - Botón "Corregir" para reabrir el pedido
 *
 * Diseño ticket-style: filas planas con divisor sutil,
 * botón siempre visible con texto claro.
 */
export default function HistorialVentas({
  ventas,
  isLoading,
  onCorregirPedido,
}: HistorialVentasProps) {

  // ── Skeleton ────────────────────────────────────────────────────────────

  if (isLoading) {
    return (
      <div className="rounded-2xl bg-neutral-800/30 overflow-hidden">
        {Array.from({ length: 4 }).map((_, i) => (
          <div
            key={i}
            className="flex items-center gap-4 px-5 py-3.5 animate-pulse border-b border-neutral-800/50 last:border-b-0"
          >
            <div className="w-8 h-8 rounded-lg bg-neutral-700" />
            <div className="h-3 w-10 rounded bg-neutral-700" />
            <div className="flex-1 space-y-1">
              <div className="h-3 w-32 rounded bg-neutral-700" />
              <div className="h-2.5 w-20 rounded bg-neutral-700" />
            </div>
            <div className="h-4 w-16 rounded bg-neutral-700" />
          </div>
        ))}
      </div>
    );
  }

  // ── Empty state ─────────────────────────────────────────────────────────

  if (!ventas || ventas.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-16 text-center">
        <ShoppingBag size={36} className="text-neutral-700 mb-3" />
        <p className="text-gray-500 font-medium text-sm">Sin ventas hoy</p>
        <p className="text-xs text-gray-600 mt-1">
          Las mesas cerradas aparecerán aquí
        </p>
      </div>
    );
  }

  // ── Lista ───────────────────────────────────────────────────────────────

  return (
    <>
      <div className="rounded-2xl bg-neutral-800/30 overflow-hidden">
        {ventas.map((venta, idx) => (
          <div
            key={venta.pedidoId}
            className={[
              'flex items-center gap-4 px-5 py-3.5',
              'hover:bg-neutral-800 transition-colors',
              idx < ventas.length - 1 ? 'border-b border-neutral-800/50' : '',
            ].join(' ')}
          >
            {/* Ícono */}
            <div className="w-8 h-8 rounded-lg bg-emerald-950/40 text-emerald-500/70 flex items-center justify-center shrink-0">
              <ShoppingBag size={15} />
            </div>

            {/* Hora */}
            <span className="text-xs font-mono text-gray-600 w-10 shrink-0 tabular-nums">
              {formatHora(venta.fechaCierre)}
            </span>

            {/* Mesa + Pedido */}
            <div className="flex-1 min-w-0">
              <p className="text-sm text-gray-300 truncate leading-tight">
                Mesa {venta.mesaNumero}
              </p>
              <p className="text-[11px] text-gray-600 font-mono truncate mt-0.5">
                Pedido #{venta.numeroPedido}
              </p>
            </div>

            {/* Monto */}
            <span className="text-sm font-semibold font-mono text-emerald-400/90 shrink-0 tabular-nums">
              +${formatMonto(venta.total)}
            </span>

            {/* Botón Corregir pedido */}
            <button
              type="button"
              onClick={() => onCorregirPedido(venta.pedidoId)}
              className={[
                'shrink-0 flex items-center gap-1.5 px-3 py-1.5 rounded-lg',
                'text-xs font-medium',
                'text-gray-400 bg-neutral-800 border border-neutral-700',
                'hover:text-red-400 hover:border-red-800/50 hover:bg-red-950/20',
                'transition-all duration-150 active:scale-95',
              ].join(' ')}
              aria-label={`Corregir pedido #${venta.numeroPedido}`}
            >
              <RotateCcw size={13} />
              Corregir
            </button>
          </div>
        ))}
      </div>
    </>
  );
}
