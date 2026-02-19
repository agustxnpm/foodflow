import { useState, useCallback, useMemo, useEffect } from 'react';
import {
  X,
  DollarSign,
  CreditCard,
  QrCode,
  ArrowRightLeft,
  Plus,
  Trash2,
  Loader2,
  Check,
  Printer,
} from 'lucide-react';
import type { DetallePedidoResponse } from '../types';
import type { TicketImpresionResponse } from '../types-impresion';
import type { MedioPago, PagoRequest } from '../../salon/types';
import { useCerrarMesa, useObtenerTicket } from '../../salon/hooks/useMesas';
import TicketPreview from './TicketPreview';
import useToast from '../../../hooks/useToast';

// ─── Tipos locales ────────────────────────────────────────────────────────────

interface PagoEntry {
  id: string;
  medio: MedioPago;
  monto: string;
}

interface CerrarMesaModalProps {
  mesaId: string;
  pedido: DetallePedidoResponse;
  onClose: () => void;
  /** Se invoca tras cierre exitoso — redirige al salón */
  onSuccess: () => void;
}

// ─── Configuración de medios de pago ──────────────────────────────────────────

const MEDIOS_PAGO: Array<{
  tipo: MedioPago;
  icono: typeof DollarSign;
  label: string;
  color: string;
}> = [
  { tipo: 'EFECTIVO', icono: DollarSign, label: 'Efectivo', color: 'text-green-400' },
  { tipo: 'TARJETA', icono: CreditCard, label: 'Tarjeta', color: 'text-blue-400' },
  { tipo: 'TRANSFERENCIA', icono: ArrowRightLeft, label: 'Transfer.', color: 'text-purple-400' },
  { tipo: 'QR', icono: QrCode, label: 'QR', color: 'text-yellow-400' },
];

// ─── Helpers ──────────────────────────────────────────────────────────────────

let _nextPagoId = 0;
function nextPagoId(): string {
  return `pago-${++_nextPagoId}`;
}

// ─── Componente ───────────────────────────────────────────────────────────────

/**
 * Modal de cierre de mesa — Split View
 *
 * Columna Izquierda: Preview del ticket estilo impresora térmica
 * Columna Derecha: Procesador de pagos con soporte para split payment
 *
 * Regla del backend: la suma de pagos DEBE coincidir exactamente con el total.
 * Si el medio incluye EFECTIVO y la suma lo supera → se muestra vuelto.
 *
 * HU-04: Cerrar mesa
 * HU-12: Cierre de mesa y liquidación final
 * HU-29: Ticket de venta
 */
export default function CerrarMesaModal({
  mesaId,
  pedido,
  onClose,
  onSuccess,
}: CerrarMesaModalProps) {
  const toast = useToast();
  const cerrarMesa = useCerrarMesa();
  const obtenerTicket = useObtenerTicket();

  const total = pedido.totalParcial;

  // ── Estado del ticket preview ──
  const [ticketData, setTicketData] = useState<TicketImpresionResponse | null>(null);
  const [cargandoTicket, setCargandoTicket] = useState(true);

  // ── Estado de pagos ──
  const [pagos, setPagos] = useState<PagoEntry[]>([
    { id: nextPagoId(), medio: 'EFECTIVO', monto: '' },
  ]);

  // ── Cargar ticket preview al montar ──
  useEffect(() => {
    obtenerTicket.mutate(mesaId, {
      onSuccess: (data) => {
        setTicketData(data);
        setCargandoTicket(false);
      },
      onError: () => {
        setCargandoTicket(false);
        // Ticket preview es best-effort, no bloquea el cierre
      },
    });
    // Solo al montar
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mesaId]);

  // ── Cálculos de pago ──
  const sumaPagos = useMemo(
    () => pagos.reduce((acc, p) => acc + (parseFloat(p.monto) || 0), 0),
    [pagos]
  );

  const diferencia = sumaPagos - total;
  const tieneEfectivo = pagos.some((p) => p.medio === 'EFECTIVO');
  const hayVuelto = diferencia > 0 && tieneEfectivo;

  // El cierre es válido cuando la suma cubre exactamente el total,
  // o lo supera sólo si hay efectivo (vuelto)
  const pagoValido = useMemo(() => {
    if (sumaPagos < total) return false;
    if (sumaPagos > total && !tieneEfectivo) return false;
    // Todos los pagos deben tener monto > 0
    return pagos.every((p) => parseFloat(p.monto) > 0);
  }, [sumaPagos, total, tieneEfectivo, pagos]);

  // ── Handlers de pagos ──

  const handleAgregarPago = useCallback(() => {
    setPagos((prev) => [
      ...prev,
      { id: nextPagoId(), medio: 'EFECTIVO', monto: '' },
    ]);
  }, []);

  const handleEliminarPago = useCallback((pagoId: string) => {
    setPagos((prev) => (prev.length <= 1 ? prev : prev.filter((p) => p.id !== pagoId)));
  }, []);

  const handleMedioChange = useCallback((pagoId: string, medio: MedioPago) => {
    setPagos((prev) =>
      prev.map((p) => (p.id === pagoId ? { ...p, medio } : p))
    );
  }, []);

  const handleMontoChange = useCallback((pagoId: string, monto: string) => {
    setPagos((prev) =>
      prev.map((p) => (p.id === pagoId ? { ...p, monto } : p))
    );
  }, []);

  /** Atajo: pagar todo con un solo método */
  const handlePagoRapido = useCallback(
    (medio: MedioPago) => {
      setPagos([
        { id: nextPagoId(), medio, monto: total.toFixed(2) },
      ]);
    },
    [total]
  );

  // ── Handler de cierre ──
  const handleConfirmar = useCallback(() => {
    if (!pagoValido) return;

    // Construir pagos para el backend
    // Si hay vuelto en efectivo, ajustar el monto del pago en efectivo al total restante
    const pagosRequest: PagoRequest[] = pagos.map((p) => ({
      medio: p.medio,
      monto: parseFloat(p.monto),
    }));

    // Si la suma supera el total y hay efectivo, ajustar
    if (diferencia > 0 && tieneEfectivo) {
      const totalSinEfectivo = pagosRequest
        .filter((p) => p.medio !== 'EFECTIVO')
        .reduce((acc, p) => acc + p.monto, 0);
      const montoEfectivoNecesario = total - totalSinEfectivo;

      for (const p of pagosRequest) {
        if (p.medio === 'EFECTIVO') {
          p.monto = Math.round(montoEfectivoNecesario * 100) / 100;
          break;
        }
      }
    }

    cerrarMesa.mutate(
      { mesaId, pagos: pagosRequest },
      {
        onSuccess: () => {
          toast.success(`Mesa ${pedido.numeroMesa} cerrada exitosamente`);
          onSuccess();
        },
        onError: (error: any) => {
          const msg =
            error?.response?.data?.message || 'Error al cerrar la mesa';
          toast.error(msg);
        },
      }
    );
  }, [
    pagoValido,
    pagos,
    diferencia,
    tieneEfectivo,
    total,
    cerrarMesa,
    mesaId,
    pedido.numeroMesa,
    toast,
    onSuccess,
  ]);

  const isPending = cerrarMesa.isPending;

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 z-[80] bg-black/70 backdrop-blur-sm animate-backdrop-in"
        aria-hidden="true"
      />

      {/* Modal */}
      <div className="fixed inset-0 z-[90] flex items-center justify-center p-4 pointer-events-none">
        <div
          className="
            pointer-events-auto
            w-full max-w-4xl max-h-[85vh]
            bg-neutral-900 rounded-2xl
            border border-neutral-700
            shadow-2xl shadow-black/60
            animate-modal-in
            flex flex-col
          "
        >
          {/* ── Header ── */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-neutral-800 shrink-0">
            <div>
              <h2 className="text-lg font-bold text-gray-100">
                Cerrar Mesa {pedido.numeroMesa}
              </h2>
              <p className="text-xs text-gray-500 mt-0.5">
                Pedido #{pedido.numeroPedido} · {pedido.items.length} ítems
              </p>
            </div>
            <button
              type="button"
              onClick={onClose}
              disabled={isPending}
              className="text-gray-500 hover:text-gray-300 transition-colors disabled:opacity-50"
            >
              <X size={20} />
            </button>
          </div>

          {/* ── Body: Split View ── */}
          <div className="flex-1 flex overflow-hidden">
            {/* Columna Izquierda: Ticket Preview */}
            <div className="w-[40%] border-r border-neutral-800 overflow-y-auto p-5 bg-neutral-950/50">
              <div className="flex items-center gap-2 mb-4">
                <Printer size={14} className="text-gray-500" />
                <span className="text-xs font-semibold text-gray-500 uppercase tracking-widest">
                  Preview del Ticket
                </span>
              </div>

              {cargandoTicket ? (
                <div className="flex items-center justify-center h-60">
                  <Loader2 size={24} className="animate-spin text-gray-600" />
                </div>
              ) : ticketData ? (
                <TicketPreview ticket={ticketData} />
              ) : (
                <div className="flex items-center justify-center h-60 text-gray-600 text-sm">
                  No se pudo cargar el ticket
                </div>
              )}
            </div>

            {/* Columna Derecha: Procesador de Pago */}
            <div className="w-[60%] overflow-y-auto p-5 space-y-5">
              {/* Total grande */}
              <div className="text-center py-3 bg-neutral-800/50 rounded-xl border border-neutral-700/50">
                <p className="text-xs text-gray-500 uppercase tracking-widest mb-1">
                  Total a Pagar
                </p>
                <p className="text-3xl font-bold text-red-500 font-mono tabular-nums">
                  $ {total.toLocaleString('es-AR', { minimumFractionDigits: 2 })}
                </p>
              </div>

              {/* Atajos de pago rápido */}
              <div>
                <p className="text-xs font-semibold text-gray-500 uppercase tracking-widest mb-2">
                  Pago rápido (un solo método)
                </p>
                <div className="grid grid-cols-4 gap-2">
                  {MEDIOS_PAGO.map(({ tipo, icono: Icon, label, color }) => (
                    <button
                      key={tipo}
                      type="button"
                      onClick={() => handlePagoRapido(tipo)}
                      disabled={isPending}
                      className="
                        flex flex-col items-center justify-center gap-1.5
                        h-16 rounded-xl
                        bg-neutral-800 border border-neutral-700
                        hover:border-neutral-500 hover:bg-neutral-750
                        disabled:opacity-50 disabled:cursor-not-allowed
                        transition-all active:scale-95
                      "
                    >
                      <Icon size={18} className={color} />
                      <span className="text-[10px] font-semibold text-gray-400">
                        {label}
                      </span>
                    </button>
                  ))}
                </div>
              </div>

              {/* Split Payment */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <p className="text-xs font-semibold text-gray-500 uppercase tracking-widest">
                    Desglose de Pagos
                  </p>
                  <button
                    type="button"
                    onClick={handleAgregarPago}
                    disabled={isPending}
                    className="
                      flex items-center gap-1
                      text-xs font-semibold text-red-400
                      hover:text-red-300
                      disabled:opacity-50
                      transition-colors
                    "
                  >
                    <Plus size={12} />
                    Agregar pago
                  </button>
                </div>

                <div className="space-y-2">
                  {pagos.map((pago, idx) => (
                    <div
                      key={pago.id}
                      className="flex items-center gap-2 bg-neutral-800/50 rounded-xl p-2 border border-neutral-700/50"
                    >
                      {/* Selector de medio */}
                      <select
                        value={pago.medio}
                        onChange={(e) =>
                          handleMedioChange(pago.id, e.target.value as MedioPago)
                        }
                        disabled={isPending}
                        className="
                          h-10 px-2 rounded-lg
                          bg-neutral-800 border border-neutral-700
                          text-sm text-gray-200
                          focus:outline-none focus:border-red-600
                          disabled:opacity-50
                        "
                      >
                        {MEDIOS_PAGO.map(({ tipo, label }) => (
                          <option key={tipo} value={tipo}>
                            {label}
                          </option>
                        ))}
                      </select>

                      {/* Input monto */}
                      <div className="relative flex-1">
                        <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500 text-sm">
                          $
                        </span>
                        <input
                          type="number"
                          min={0}
                          step={0.01}
                          value={pago.monto}
                          onChange={(e) =>
                            handleMontoChange(pago.id, e.target.value)
                          }
                          placeholder="0.00"
                          disabled={isPending}
                          className="
                            w-full h-10 pl-7 pr-3
                            bg-neutral-800 border border-neutral-700
                            rounded-lg text-sm font-mono text-gray-100
                            placeholder:text-gray-600
                            focus:outline-none focus:border-red-600
                            disabled:opacity-50
                            tabular-nums
                          "
                        />
                      </div>

                      {/* Eliminar (solo si hay más de 1) */}
                      {pagos.length > 1 && (
                        <button
                          type="button"
                          onClick={() => handleEliminarPago(pago.id)}
                          disabled={isPending}
                          className="
                            w-9 h-9 rounded-lg shrink-0
                            flex items-center justify-center
                            text-gray-600 hover:text-red-400 hover:bg-red-950/30
                            transition-colors disabled:opacity-50
                          "
                          aria-label={`Eliminar pago ${idx + 1}`}
                        >
                          <Trash2 size={14} />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              </div>

              {/* Resumen de pagos */}
              <div className="bg-neutral-800/30 rounded-xl border border-neutral-700/50 px-4 py-3 space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-gray-400">Total pagos</span>
                  <span
                    className={[
                      'font-mono tabular-nums font-semibold',
                      sumaPagos >= total ? 'text-green-400' : 'text-gray-300',
                    ].join(' ')}
                  >
                    $ {sumaPagos.toLocaleString('es-AR', { minimumFractionDigits: 2 })}
                  </span>
                </div>

                {diferencia < 0 && (
                  <div className="flex justify-between text-sm">
                    <span className="text-yellow-400/80">Falta pagar</span>
                    <span className="font-mono tabular-nums text-yellow-400 font-bold">
                      $ {Math.abs(diferencia).toLocaleString('es-AR', { minimumFractionDigits: 2 })}
                    </span>
                  </div>
                )}

                {hayVuelto && (
                  <div className="flex justify-between text-sm">
                    <span className="text-green-400/80">Vuelto (Efectivo)</span>
                    <span className="font-mono tabular-nums text-green-400 font-bold">
                      $ {diferencia.toLocaleString('es-AR', { minimumFractionDigits: 2 })}
                    </span>
                  </div>
                )}

                {diferencia > 0 && !tieneEfectivo && (
                  <p className="text-xs text-red-400">
                    El monto supera el total. Solo se puede dar vuelto con Efectivo.
                  </p>
                )}
              </div>
            </div>
          </div>

          {/* ── Footer ── */}
          <div className="px-6 py-4 border-t border-neutral-800 flex gap-3 shrink-0">
            <button
              type="button"
              onClick={onClose}
              disabled={isPending}
              className="
                flex-1 h-12 rounded-xl
                text-sm font-semibold
                bg-neutral-800 text-gray-400
                border border-neutral-700
                hover:border-neutral-600 hover:text-gray-300
                transition-colors active:scale-[0.97]
                disabled:opacity-50
              "
            >
              Cancelar
            </button>
            <button
              type="button"
              onClick={handleConfirmar}
              disabled={!pagoValido || isPending}
              className="
                flex-[2] h-12 rounded-xl
                flex items-center justify-center gap-2
                text-base font-bold
                bg-red-600 text-white
                hover:bg-red-500
                disabled:bg-neutral-700 disabled:text-gray-500 disabled:cursor-not-allowed
                transition-colors active:scale-[0.97]
                shadow-sm shadow-red-950/40
              "
            >
              {isPending ? (
                <>
                  <Loader2 size={18} className="animate-spin" />
                  Cerrando…
                </>
              ) : (
                <>
                  <Check size={18} />
                  Confirmar y Cerrar Mesa
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </>
  );
}
