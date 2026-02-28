import { useState } from 'react';
import { RotateCcw, Receipt, Loader2, AlertTriangle, ArrowDownCircle, ShoppingBag } from 'lucide-react';
import type { MovimientoResumen } from '../types';

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

// ─── Modal de confirmación de reapertura ──────────────────────────────────────

interface ConfirmacionReabrirProps {
  movimiento: MovimientoResumen;
  onConfirmar: () => void;
  onCancelar: () => void;
  isPending: boolean;
}

function ConfirmacionReabrir({
  movimiento,
  onConfirmar,
  onCancelar,
  isPending,
}: ConfirmacionReabrirProps) {
  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 z-[60] bg-black/70 backdrop-blur-sm animate-backdrop-in"
        onClick={onCancelar}
        aria-hidden="true"
      />

      {/* Modal */}
      <div className="fixed inset-0 z-[70] flex items-center justify-center p-4">
        <div
          className={[
            'bg-neutral-900 border-2 border-red-800 rounded-2xl',
            'shadow-2xl shadow-black/60',
            'w-full max-w-sm p-6 text-center space-y-4',
            'animate-modal-in',
          ].join(' ')}
          role="alertdialog"
          aria-modal="true"
          aria-labelledby="reabrir-titulo"
          aria-describedby="reabrir-desc"
        >
          <div className="mx-auto w-16 h-16 rounded-full bg-red-950/60 flex items-center justify-center">
            <AlertTriangle size={32} className="text-red-400" />
          </div>

          <h3 id="reabrir-titulo" className="text-lg font-bold text-gray-100">
            ¿Reabrir este pedido?
          </h3>

          <p id="reabrir-desc" className="text-sm text-gray-400 leading-relaxed">
            Esto revertirá los pagos registrados de{' '}
            <span className="text-gray-200 font-semibold font-mono">
              ${formatMonto(movimiento.monto)}
            </span>{' '}
            y la mesa volverá a estado{' '}
            <span className="text-red-400 font-semibold">ABIERTA</span>.
          </p>

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onCancelar}
              disabled={isPending}
              className={[
                'flex-1 h-12 rounded-xl font-medium text-sm',
                'bg-neutral-800 border border-neutral-700 text-gray-300',
                'hover:bg-neutral-700 transition-colors active:scale-95',
              ].join(' ')}
            >
              Cancelar
            </button>
            <button
              type="button"
              onClick={onConfirmar}
              disabled={isPending}
              className={[
                'flex-1 h-12 rounded-xl font-semibold text-sm',
                'flex items-center justify-center gap-2',
                'transition-all duration-150',
                isPending
                  ? 'bg-neutral-700 text-gray-400 cursor-wait'
                  : 'bg-red-600 hover:bg-red-500 text-white active:scale-95',
              ].join(' ')}
            >
              {isPending ? (
                <>
                  <Loader2 size={16} className="animate-spin" />
                  Reabriendo...
                </>
              ) : (
                <>
                  <RotateCcw size={16} />
                  Sí, Reabrir
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </>
  );
}

// ─── Componente principal: Lista de movimientos ───────────────────────────────

interface ListaMovimientosDiaProps {
  movimientos: MovimientoResumen[];
  isLoading: boolean;
  onReabrirPedido: (pedidoId: string) => void;
  reabriendo: boolean;
}

/**
 * Lista tipo ticket de movimientos del día.
 *
 * Filas planas separadas por divisor sutil (sin tarjetas individuales).
 * hover:bg-neutral-800 limpio, montos alineados a la derecha, tipografía mono.
 * Botón de reabrir aparece solo en hover/focus del row para reducir ruido visual.
 */
export default function ListaMovimientosDia({
  movimientos,
  isLoading,
  onReabrirPedido,
  reabriendo,
}: ListaMovimientosDiaProps) {
  const [movimientoAReabrir, setMovimientoAReabrir] = useState<MovimientoResumen | null>(null);

  const handleConfirmarReabrir = () => {
    if (!movimientoAReabrir) return;
    onReabrirPedido(movimientoAReabrir.id);
    setMovimientoAReabrir(null);
  };

  // ── Skeleton ────────────────────────────────────────────────────────────

  if (isLoading) {
    return (
      <div className="rounded-2xl bg-neutral-800/30 overflow-hidden">
        {Array.from({ length: 5 }).map((_, i) => (
          <div
            key={i}
            className="flex items-center gap-4 px-5 py-3.5 animate-pulse border-b border-neutral-800/50 last:border-b-0"
          >
            <div className="h-3 w-10 rounded bg-neutral-700" />
            <div className="flex-1 space-y-1">
              <div className="h-3 w-40 rounded bg-neutral-700" />
              <div className="h-2.5 w-24 rounded bg-neutral-700" />
            </div>
            <div className="h-4 w-16 rounded bg-neutral-700" />
          </div>
        ))}
      </div>
    );
  }

  // ── Empty state ─────────────────────────────────────────────────────────

  if (!movimientos || movimientos.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-16 text-center">
        <Receipt size={36} className="text-neutral-700 mb-3" />
        <p className="text-gray-500 font-medium text-sm">Sin movimientos hoy</p>
        <p className="text-xs text-gray-600 mt-1">
          Los egresos y ventas aparecerán aquí
        </p>
      </div>
    );
  }

  // ── Helpers ─────────────────────────────────────────────────────────────

  const esPedido = (mov: MovimientoResumen) => mov.tipo === 'PEDIDO';

  // ── Lista ticket ────────────────────────────────────────────────────────

  return (
    <>
      <div className="rounded-2xl bg-neutral-800/30 overflow-hidden">
        {movimientos.map((mov, idx) => {
          const pedido = esPedido(mov);

          return (
            <div
              key={mov.id}
              className={[
                'flex items-center gap-4 px-5 py-3.5',
                'hover:bg-neutral-800 transition-colors',
                idx < movimientos.length - 1 ? 'border-b border-neutral-800/50' : '',
              ].join(' ')}
            >
              {/* Ícono de tipo */}
              <div
                className={[
                  'w-8 h-8 rounded-lg flex items-center justify-center shrink-0',
                  pedido
                    ? 'bg-emerald-950/40 text-emerald-500/70'
                    : 'bg-amber-950/30 text-amber-500/60',
                ].join(' ')}
              >
                {pedido ? <ShoppingBag size={15} /> : <ArrowDownCircle size={15} />}
              </div>

              {/* Hora — columna fija */}
              <span className="text-xs font-mono text-gray-600 w-10 shrink-0 tabular-nums">
                {formatHora(mov.fecha)}
              </span>

              {/* Descripción + comprobante */}
              <div className="flex-1 min-w-0">
                <p className="text-sm text-gray-300 truncate leading-tight">
                  {mov.descripcion}
                </p>
                <p className="text-[11px] text-gray-600 font-mono truncate mt-0.5">
                  {mov.numeroComprobante}
                </p>
              </div>

              {/* Monto — alineado a la derecha */}
              <span
                className={[
                  'text-sm font-semibold font-mono shrink-0 tabular-nums',
                  pedido ? 'text-emerald-400/90' : 'text-amber-400/90',
                ].join(' ')}
              >
                {pedido ? '+' : '−'}${formatMonto(mov.monto)}
              </span>

              {/* Botón Corregir pedido — SOLO en pedidos, siempre visible */}
              {pedido && (
                <button
                  type="button"
                  onClick={() => setMovimientoAReabrir(mov)}
                  disabled={reabriendo}
                  className={[
                    'shrink-0 flex items-center gap-1.5 px-3 py-1.5 rounded-lg',
                    'text-xs font-medium',
                    'text-gray-400 bg-neutral-800 border border-neutral-700',
                    'hover:text-red-400 hover:border-red-800/50 hover:bg-red-950/20',
                    'transition-all duration-150 active:scale-95',
                    'disabled:opacity-40 disabled:cursor-not-allowed',
                  ].join(' ')}
                  aria-label={`Corregir pedido ${mov.numeroComprobante}`}
                >
                  <RotateCcw size={13} />
                  Corregir
                </button>
              )}
            </div>
          );
        })}
      </div>

      {/* Modal de confirmación */}
      {movimientoAReabrir && (
        <ConfirmacionReabrir
          movimiento={movimientoAReabrir}
          onConfirmar={handleConfirmarReabrir}
          onCancelar={() => setMovimientoAReabrir(null)}
          isPending={reabriendo}
        />
      )}
    </>
  );
}
