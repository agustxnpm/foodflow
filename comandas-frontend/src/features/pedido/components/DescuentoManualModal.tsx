import { useState, useCallback } from 'react';
import { X, Percent, Tag, ShoppingBag } from 'lucide-react';
import type { DetallePedidoResponse, ItemDetalle } from '../types';
import {
  useAplicarDescuentoGlobal,
  useAplicarDescuentoPorItem,
} from '../hooks/usePedido';
import useToast from '../../../hooks/useToast';

// ─── Tipos locales ────────────────────────────────────────────────────────────

type AmbitoDescuento = 'TOTAL' | 'ITEM';

interface DescuentoManualModalProps {
  pedido: DetallePedidoResponse;
  onClose: () => void;
}

// ─── Componente ───────────────────────────────────────────────────────────────

/**
 * Modal de descuento manual — HU-14
 *
 * Permite aplicar descuentos manuales al pedido:
 * - Global: porcentaje sobre el total del pedido
 * - Por Ítem: porcentaje sobre un ítem específico
 *
 * El backend recalcula montos dinámicamente.
 * El descuento no modifica precios base ni afecta la vista de cocina.
 */
export default function DescuentoManualModal({
  pedido,
  onClose,
}: DescuentoManualModalProps) {
  const toast = useToast();

  // ── Estado del formulario ──
  const [ambito, setAmbito] = useState<AmbitoDescuento>('TOTAL');
  const [porcentaje, setPorcentaje] = useState<string>('');
  const [razon, setRazon] = useState('');
  const [itemSeleccionadoId, setItemSeleccionadoId] = useState<string | null>(null);

  // ── Mutations ──
  const descuentoGlobal = useAplicarDescuentoGlobal();
  const descuentoPorItem = useAplicarDescuentoPorItem();

  const isPending = descuentoGlobal.isPending || descuentoPorItem.isPending;
  const porcentajeNum = parseFloat(porcentaje) || 0;
  const esValido =
    porcentajeNum > 0 &&
    porcentajeNum <= 100 &&
    (ambito === 'TOTAL' || itemSeleccionadoId !== null);

  // ── Handler de aplicación ──
  const handleAplicar = useCallback(() => {
    if (!esValido) return;

    const payload = {
      pedidoId: pedido.pedidoId,
      porcentaje: porcentajeNum,
      razon: razon.trim() || undefined,
      // MVP: UUID fijo para el único operador del local (sin autenticación real)
      usuarioId: '00000000-0000-0000-0000-000000000001',
    };

    const callbacks = {
      onSuccess: () => {
        toast.success(
          ambito === 'TOTAL'
            ? `Descuento del ${porcentajeNum}% aplicado al pedido`
            : `Descuento del ${porcentajeNum}% aplicado al ítem`
        );
        onClose();
      },
      onError: (error: any) => {
        const msg =
          error?.response?.data?.message || 'Error al aplicar descuento';
        toast.error(msg);
      },
    };

    if (ambito === 'TOTAL') {
      descuentoGlobal.mutate(payload, callbacks);
    } else {
      descuentoPorItem.mutate(
        { ...payload, itemId: itemSeleccionadoId! },
        callbacks
      );
    }
  }, [
    esValido,
    ambito,
    pedido.pedidoId,
    porcentajeNum,
    razon,
    itemSeleccionadoId,
    descuentoGlobal,
    descuentoPorItem,
    toast,
    onClose,
  ]);

  // ── Render helpers ──

  const renderItemOption = (item: ItemDetalle) => {
    const isSelected = itemSeleccionadoId === item.id;
    return (
      <button
        key={item.id}
        type="button"
        onClick={() => setItemSeleccionadoId(item.id)}
        className={[
          'w-full flex items-center justify-between gap-2',
          'px-3 py-2.5 rounded-lg text-left',
          'transition-all duration-150',
          isSelected
            ? 'bg-red-950/50 border border-red-600/50 ring-1 ring-red-600/30'
            : 'bg-neutral-800/60 border border-neutral-700/50 hover:border-neutral-600',
        ].join(' ')}
      >
        <div className="flex items-center gap-2 min-w-0">
          <span className="text-red-400 font-mono font-bold text-sm shrink-0">
            {item.cantidad}x
          </span>
          <span className="text-sm text-gray-200 truncate">
            {item.nombreProducto}
          </span>
        </div>
        <span className="text-sm text-gray-400 font-mono shrink-0">
          $ {item.subtotal.toLocaleString('es-AR')}
        </span>
      </button>
    );
  };

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 z-[80] bg-black/60 animate-backdrop-in"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Modal */}
      <div className="fixed inset-0 z-[90] flex items-center justify-center p-4 pointer-events-none">
        <div
          className="
            pointer-events-auto
            w-full max-w-md
            bg-neutral-900 rounded-2xl
            border border-neutral-700
            shadow-2xl shadow-black/60
            animate-modal-in
          "
        >
          {/* ── Header ── */}
          <div className="flex items-center justify-between px-5 py-4 border-b border-neutral-800">
            <div className="flex items-center gap-2">
              <Percent size={18} className="text-red-400" />
              <h2 className="text-lg font-bold text-gray-100">
                Descuento Manual
              </h2>
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

          {/* ── Body ── */}
          <div className="px-5 py-4 space-y-5">
            {/* Tabs de ámbito */}
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => {
                  setAmbito('TOTAL');
                  setItemSeleccionadoId(null);
                }}
                className={[
                  'flex-1 flex items-center justify-center gap-2',
                  'h-10 rounded-xl text-sm font-semibold',
                  'transition-all duration-150',
                  ambito === 'TOTAL'
                    ? 'bg-red-600 text-white shadow-sm shadow-red-950/40'
                    : 'bg-neutral-800 text-gray-400 border border-neutral-700 hover:text-gray-300',
                ].join(' ')}
              >
                <ShoppingBag size={15} />
                Todo el pedido
              </button>
              <button
                type="button"
                onClick={() => setAmbito('ITEM')}
                className={[
                  'flex-1 flex items-center justify-center gap-2',
                  'h-10 rounded-xl text-sm font-semibold',
                  'transition-all duration-150',
                  ambito === 'ITEM'
                    ? 'bg-red-600 text-white shadow-sm shadow-red-950/40'
                    : 'bg-neutral-800 text-gray-400 border border-neutral-700 hover:text-gray-300',
                ].join(' ')}
              >
                <Tag size={15} />
                Un ítem
              </button>
            </div>

            {/* Selector de ítem (solo si ámbito = ITEM) */}
            {ambito === 'ITEM' && (
              <div className="space-y-2 max-h-40 overflow-y-auto pr-1">
                <label className="text-xs font-semibold text-gray-500 uppercase tracking-widest">
                  Seleccioná el ítem
                </label>
                {pedido.items.map(renderItemOption)}
              </div>
            )}

            {/* Input de porcentaje */}
            <div className="space-y-1.5">
              <label
                htmlFor="porcentaje"
                className="text-xs font-semibold text-gray-500 uppercase tracking-widest"
              >
                Porcentaje de descuento
              </label>
              <div className="relative">
                <input
                  id="porcentaje"
                  type="number"
                  min={0}
                  max={100}
                  step={1}
                  value={porcentaje}
                  onChange={(e) => setPorcentaje(e.target.value)}
                  placeholder="Ej: 10"
                  className="
                    w-full h-12 pl-4 pr-10
                    bg-neutral-800 border border-neutral-700
                    rounded-xl text-lg font-mono text-gray-100
                    placeholder:text-gray-600
                    focus:outline-none focus:border-red-600 focus:ring-1 focus:ring-red-600/30
                    transition-colors
                  "
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 font-bold text-lg">
                  %
                </span>
              </div>
              {porcentajeNum > 100 && (
                <p className="text-xs text-red-400">
                  El porcentaje no puede superar el 100%
                </p>
              )}
            </div>

            {/* Input de razón */}
            <div className="space-y-1.5">
              <label
                htmlFor="razon"
                className="text-xs font-semibold text-gray-500 uppercase tracking-widest"
              >
                Motivo{' '}
              </label>
              <input
                id="razon"
                type="text"
                value={razon}
                onChange={(e) => setRazon(e.target.value)}
                placeholder="Ej: Cliente frecuente, compensación..."
                className="
                  w-full h-10 px-4
                  bg-neutral-800 border border-neutral-700
                  rounded-xl text-sm text-gray-200
                  placeholder:text-gray-600
                  focus:outline-none focus:border-red-600 focus:ring-1 focus:ring-red-600/30
                  transition-colors
                "
              />
            </div>

            {/* Preview del descuento */}
            {esValido && (
              <div className="bg-neutral-800/50 border border-neutral-700/50 rounded-xl px-4 py-3">
                <p className="text-xs text-gray-500 mb-1">Vista previa</p>
                <p className="text-sm text-gray-300">
                  {ambito === 'TOTAL' ? (
                    <>
                      <span className="text-red-400 font-bold">{porcentajeNum}%</span>
                      {' '}sobre el total del pedido
                      {' '}
                      <span className="text-gray-500 font-mono">
                        ($ {pedido.totalParcial.toLocaleString('es-AR')})
                      </span>
                    </>
                  ) : (
                    <>
                      <span className="text-red-400 font-bold">{porcentajeNum}%</span>
                      {' '}sobre{' '}
                      <span className="font-medium">
                        {pedido.items.find((i) => i.id === itemSeleccionadoId)?.nombreProducto}
                      </span>
                    </>
                  )}
                </p>
              </div>
            )}
          </div>

          {/* ── Footer ── */}
          <div className="px-5 py-4 border-t border-neutral-800 flex gap-3">
            <button
              type="button"
              onClick={onClose}
              disabled={isPending}
              className="
                flex-1 h-11 rounded-xl
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
              onClick={handleAplicar}
              disabled={!esValido || isPending}
              className="
                flex-1 h-11 rounded-xl
                text-sm font-bold
                bg-red-600 text-white
                hover:bg-red-500
                disabled:bg-neutral-700 disabled:text-gray-500 disabled:cursor-not-allowed
                transition-colors active:scale-[0.97]
                shadow-sm shadow-red-950/40
              "
            >
              {isPending ? 'Aplicando…' : 'Aplicar Descuento'}
            </button>
          </div>
        </div>
      </div>
    </>
  );
}
