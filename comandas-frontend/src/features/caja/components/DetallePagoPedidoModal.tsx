import {
  X,
  ShoppingBag,
  MapPin,
  Clock,
  CreditCard,
  DollarSign,
  QrCode,
  ArrowRightLeft,
  Users,
} from 'lucide-react';
import type { PagoDetalle, VentaResumen } from '../types';
import type { MedioPago } from '../../salon/types';

// ─── Utilidades ───────────────────────────────────────────────────────────────

function fmt(valor: number): string {
  return valor.toLocaleString('es-AR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function formatFechaHora(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleString('es-AR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

const MEDIO_LABELS: Record<MedioPago, { label: string; icon: React.ReactNode; color: string }> = {
  EFECTIVO: { label: 'Efectivo', icon: <DollarSign size={16} />, color: 'text-emerald-400' },
  TARJETA: { label: 'Tarjeta', icon: <CreditCard size={16} />, color: 'text-blue-400' },
  QR: { label: 'QR', icon: <QrCode size={16} />, color: 'text-violet-400' },
  TRANSFERENCIA: { label: 'Transferencia', icon: <ArrowRightLeft size={16} />, color: 'text-cyan-400' },
  A_CUENTA: { label: 'A cuenta', icon: <Users size={16} />, color: 'text-gray-500' },
};

// ─── Props ────────────────────────────────────────────────────────────────────

interface DetallePagoPedidoModalProps {
  pago: PagoDetalle;
  /** VentaResumen del pedido asociado (para mostrar total general) */
  venta: VentaResumen | undefined;
  /** Todos los pagos de este mismo pedido */
  pagosDelPedido: PagoDetalle[];
  onClose: () => void;
}

/**
 * Modal sencillo que muestra el contexto del pago seleccionado:
 * a qué pedido/mesa pertenece, cuándo se registró, qué medio se usó,
 * y el desglose completo de pagos del mismo pedido.
 */
export default function DetallePagoPedidoModal({
  pago,
  venta,
  pagosDelPedido,
  onClose,
}: DetallePagoPedidoModalProps) {
  const medioInfo = MEDIO_LABELS[pago.medioPago];

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 z-[60] bg-black/70 backdrop-blur-sm"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Modal */}
      <div className="fixed inset-0 z-[70] flex items-center justify-center p-4">
        <div
          className="bg-neutral-900 border border-neutral-700 rounded-2xl shadow-2xl shadow-black/60 w-full max-w-md overflow-hidden"
          role="dialog"
          aria-modal="true"
          aria-labelledby="detalle-titulo"
        >
          {/* Header */}
          <div className="flex items-center justify-between px-5 py-4 border-b border-neutral-800">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-neutral-800 flex items-center justify-center">
                <ShoppingBag size={20} className="text-gray-400" />
              </div>
              <div>
                <h3 id="detalle-titulo" className="text-base font-bold text-gray-100">
                  Pedido #{pago.numeroPedido}
                </h3>
                <p className="text-xs text-gray-500">Detalle del ingreso</p>
              </div>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="p-2 rounded-lg text-gray-500 hover:text-gray-300 hover:bg-neutral-800 transition-colors"
              aria-label="Cerrar"
            >
              <X size={18} />
            </button>
          </div>

          {/* Contenido */}
          <div className="px-5 py-5 space-y-4">
            {/* Info principal */}
            <div className="grid grid-cols-2 gap-3">
              <div className="flex items-center gap-2.5 px-3 py-2.5 rounded-xl bg-neutral-800/50">
                <MapPin size={15} className="text-gray-500 shrink-0" />
                <div>
                  <p className="text-[10px] text-gray-600 uppercase tracking-wide">Mesa</p>
                  <p className="text-sm font-semibold text-gray-200">{pago.mesaNumero}</p>
                </div>
              </div>
              <div className="flex items-center gap-2.5 px-3 py-2.5 rounded-xl bg-neutral-800/50">
                <Clock size={15} className="text-gray-500 shrink-0" />
                <div>
                  <p className="text-[10px] text-gray-600 uppercase tracking-wide">Fecha</p>
                  <p className="text-sm font-medium text-gray-200">{formatFechaHora(pago.fecha)}</p>
                </div>
              </div>
            </div>

            {/* Pago seleccionado highlight */}
            <div className="rounded-xl border border-neutral-700 bg-neutral-800/30 p-4">
              <p className="text-[10px] text-gray-600 uppercase tracking-wide mb-2">
                Pago seleccionado
              </p>
              <div className="flex items-center gap-3">
                <div className={`shrink-0 ${medioInfo.color}`}>
                  {medioInfo.icon}
                </div>
                <span className="text-sm text-gray-300 flex-1">{medioInfo.label}</span>
                <span className={`text-lg font-bold font-mono tabular-nums ${medioInfo.color}`}>
                  ${fmt(pago.monto)}
                </span>
              </div>
            </div>

            {/* Todos los pagos del pedido */}
            {pagosDelPedido.length > 1 && (
              <div className="space-y-2">
                <p className="text-[10px] text-gray-600 uppercase tracking-wide">
                  Todos los pagos de este pedido
                </p>
                <div className="rounded-xl bg-neutral-800/20 divide-y divide-neutral-800/50">
                  {pagosDelPedido.map((p, idx) => {
                    const info = MEDIO_LABELS[p.medioPago];
                    const esCurrent = p.medioPago === pago.medioPago && p.monto === pago.monto && p.fecha === pago.fecha;
                    return (
                      <div
                        key={idx}
                        className={`flex items-center gap-3 px-3 py-2.5 ${esCurrent ? 'bg-neutral-800/40' : ''}`}
                      >
                        <div className={`shrink-0 ${info.color}`}>{info.icon}</div>
                        <span className="text-sm text-gray-400 flex-1">{info.label}</span>
                        <span className={`text-sm font-mono font-semibold tabular-nums ${info.color}`}>
                          ${fmt(p.monto)}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Total del pedido */}
            {venta && (
              <div className="flex items-center justify-between pt-3 border-t border-neutral-800">
                <span className="text-sm text-gray-400">Total del pedido</span>
                <span className="text-xl font-bold font-mono tabular-nums text-gray-100">
                  ${fmt(venta.total)}
                </span>
              </div>
            )}
          </div>

          {/* Footer */}
          <div className="px-5 pb-5">
            <button
              type="button"
              onClick={onClose}
              className={[
                'w-full h-11 rounded-xl text-sm font-medium',
                'bg-neutral-800 border border-neutral-700 text-gray-300',
                'hover:bg-neutral-700 transition-colors active:scale-[0.98]',
              ].join(' ')}
            >
              Cerrar
            </button>
          </div>
        </div>
      </div>
    </>
  );
}
