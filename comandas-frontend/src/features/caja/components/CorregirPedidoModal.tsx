import { useState, useEffect, useMemo, useCallback } from 'react';
import {
  X,
  Minus,
  Plus,
  Trash2,
  PlusCircle,
  Loader2,
  Check,
  AlertTriangle,
  Tag,
} from 'lucide-react';

import { useDetallePedidoCerrado, useCorregirPedido } from '../hooks/useCaja';
import type { MedioPago } from '../../salon/types';

// ─── Constantes ───────────────────────────────────────────────────────────────

const MEDIOS_PAGO: { value: MedioPago; label: string }[] = [
  { value: 'EFECTIVO', label: 'Efectivo' },
  { value: 'TARJETA', label: 'Tarjeta' },
  { value: 'TRANSFERENCIA', label: 'Transferencia' },
  { value: 'QR', label: 'QR' },
  { value: 'A_CUENTA', label: 'A Cuenta' },
];

// ─── Estado local ─────────────────────────────────────────────────────────────

interface ItemEditable {
  itemId: string;
  nombreProducto: string;
  precioUnitario: number;
  cantidadOriginal: number;
  cantidad: number;
  observacion: string | null;
  /** Subtotal bruto original (base + extras, sin descuentos) */
  subtotalLineaOriginal: number;
  /** Monto descuento original para esta línea */
  montoDescuentoOriginal: number;
  /** Descripción legible del descuento */
  descripcionDescuento: string | null;
}

interface PagoEditable {
  key: string;
  medio: MedioPago;
  monto: string; // string para el input
}

// ─── Utilidades ───────────────────────────────────────────────────────────────

function fmt(valor: number): string {
  return valor.toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

let pagoKeyCounter = 0;
function nextPagoKey(): string {
  return `pago-${++pagoKeyCounter}`;
}

// ─── Props ────────────────────────────────────────────────────────────────────

interface CorregirPedidoModalProps {
  pedidoId: string;
  onClose: () => void;
  onCorreccionExitosa: () => void;
}

// ─── Componente ───────────────────────────────────────────────────────────────

/**
 * Modal de corrección in-place de un pedido cerrado.
 *
 * Permite ajustar cantidades de ítems y pagos sin reabrir la mesa.
 * El pedido permanece CERRADO — solo se actualiza el snapshot contable.
 *
 * Diseño: modal full-height con 2 secciones (ítems + pagos) y footer fijo.
 */
export default function CorregirPedidoModal({
  pedidoId,
  onClose,
  onCorreccionExitosa,
}: CorregirPedidoModalProps) {
  const { data: detalle, isLoading, isError } = useDetallePedidoCerrado(pedidoId);
  const corregir = useCorregirPedido();

  const [items, setItems] = useState<ItemEditable[]>([]);
  const [pagos, setPagos] = useState<PagoEditable[]>([]);
  const [error, setError] = useState<string | null>(null);

  // ── Inicializar estado desde detalle cargado ──
  useEffect(() => {
    if (!detalle) return;
    setItems(
      detalle.items.map((i) => ({
        itemId: i.itemId,
        nombreProducto: i.nombreProducto,
        precioUnitario: i.precioUnitario,
        cantidadOriginal: i.cantidad,
        cantidad: i.cantidad,
        observacion: i.observacion,
        subtotalLineaOriginal: i.subtotalLinea,
        montoDescuentoOriginal: i.montoDescuento,
        descripcionDescuento: i.descripcionDescuento,
      })),
    );
    setPagos(
      detalle.pagos.map((p) => ({
        key: nextPagoKey(),
        medio: p.medio,
        monto: p.monto.toString(),
      })),
    );
  }, [detalle]);

  // ── Cálculos derivados ──
  // Subtotal bruto escalado proporcionalmente por cantidad
  const subtotalBruto = useMemo(
    () =>
      items.reduce((acc, i) => {
        if (i.cantidadOriginal === 0) return acc;
        return acc + (i.subtotalLineaOriginal / i.cantidadOriginal) * i.cantidad;
      }, 0),
    [items],
  );

  // Descuentos escalados proporcionalmente por cantidad
  const totalDescuentos = useMemo(() => {
    if (!detalle) return 0;
    // Descuentos a nivel de ítems (escalan con cantidad)
    const descItems = items.reduce((acc, i) => {
      if (i.cantidadOriginal === 0) return acc;
      return acc + (i.montoDescuentoOriginal / i.cantidadOriginal) * i.cantidad;
    }, 0);
    // Descuento global: proporción original aplicada al nuevo subtotal neto
    const sumaNetosOriginales = detalle.items.reduce(
      (acc, i) => acc + (i.subtotalLinea - i.montoDescuento),
      0,
    );
    const descuentoGlobalOriginal =
      sumaNetosOriginales > 0 ? sumaNetosOriginales - detalle.montoTotal : 0;
    const ratioGlobal =
      sumaNetosOriginales > 0 ? descuentoGlobalOriginal / sumaNetosOriginales : 0;
    const sumaNuevosNetos = subtotalBruto - descItems;
    const descGlobalAproximado = sumaNuevosNetos * ratioGlobal;

    return descItems + descGlobalAproximado;
  }, [items, detalle, subtotalBruto]);

  // Total neto estimado (lo que los pagos deben cubrir)
  const totalNeto = subtotalBruto - totalDescuentos;

  const totalPagos = useMemo(
    () => pagos.reduce((acc, p) => acc + (parseFloat(p.monto) || 0), 0),
    [pagos],
  );

  const diferencia = totalPagos - totalNeto;
  const hayDescuentos = totalDescuentos > 0.01;
  const esValido = items.length > 0 && pagos.length > 0 && Math.abs(diferencia) < 0.01;

  const hayCambios = useMemo(() => {
    if (!detalle) return false;
    // Verificar cambios en ítems
    const itemsCambiaron = items.some((i) => i.cantidad !== i.cantidadOriginal);
    const itemsEliminados = detalle.items.length !== items.length;
    // Verificar cambios en pagos
    const pagosCambiaron =
      pagos.length !== detalle.pagos.length ||
      pagos.some((p, idx) => {
        const orig = detalle.pagos[idx];
        if (!orig) return true;
        return p.medio !== orig.medio || parseFloat(p.monto) !== orig.monto;
      });
    return itemsCambiaron || itemsEliminados || pagosCambiaron;
  }, [items, pagos, detalle]);

  // ── Handlers de ítems ──
  const cambiarCantidad = useCallback((itemId: string, delta: number) => {
    setItems((prev) =>
      prev.map((i) =>
        i.itemId === itemId ? { ...i, cantidad: Math.max(1, i.cantidad + delta) } : i,
      ),
    );
    setError(null);
  }, []);

  const eliminarItem = useCallback((itemId: string) => {
    setItems((prev) => {
      const nuevos = prev.filter((i) => i.itemId !== itemId);
      if (nuevos.length === 0) {
        setError('Debe quedar al menos un ítem');
        return prev;
      }
      return nuevos;
    });
    setError(null);
  }, []);

  // ── Handlers de pagos ──
  const cambiarMedioPago = useCallback((key: string, medio: MedioPago) => {
    setPagos((prev) => prev.map((p) => (p.key === key ? { ...p, medio } : p)));
    setError(null);
  }, []);

  const cambiarMontoPago = useCallback((key: string, monto: string) => {
    setPagos((prev) => prev.map((p) => (p.key === key ? { ...p, monto } : p)));
    setError(null);
  }, []);

  const eliminarPago = useCallback((key: string) => {
    setPagos((prev) => {
      if (prev.length <= 1) return prev;
      return prev.filter((p) => p.key !== key);
    });
    setError(null);
  }, []);

  const agregarPago = useCallback(() => {
    setPagos((prev) => [...prev, { key: nextPagoKey(), medio: 'EFECTIVO', monto: '0' }]);
    setError(null);
  }, []);

  const ajustarPagoAlTotal = useCallback(() => {
    if (pagos.length === 1) {
      setPagos((prev) => [{ ...prev[0], monto: totalNeto.toFixed(2) }]);
    }
    setError(null);
  }, [pagos.length, totalNeto]);

  // ── Submit ──
  const handleGuardar = () => {
    if (!esValido || !hayCambios) return;
    setError(null);

    corregir.mutate(
      {
        pedidoId,
        data: {
          items: items.map((i) => ({ itemId: i.itemId, cantidad: i.cantidad })),
          pagos: pagos.map((p) => ({ medio: p.medio, monto: parseFloat(p.monto) || 0 })),
        },
      },
      {
        onSuccess: () => onCorreccionExitosa(),
        onError: (err) => setError(err.message || 'Error al guardar la corrección'),
      },
    );
  };

  // ── Render ──
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
          className={[
            'bg-neutral-900 border border-neutral-700 rounded-2xl',
            'shadow-2xl shadow-black/60',
            'w-full max-w-lg max-h-[90vh] flex flex-col',
          ].join(' ')}
          role="dialog"
          aria-modal="true"
          aria-labelledby="corregir-titulo"
          onClick={(e) => e.stopPropagation()}
        >
          {/* ── Header ── */}
          <div className="flex items-center justify-between px-5 py-4 border-b border-neutral-800 shrink-0">
            <div>
              <h2 id="corregir-titulo" className="text-base font-bold text-gray-100">
                Corregir Pedido #{detalle?.numeroPedido ?? '...'}
              </h2>
              {detalle && (
                <p className="text-xs text-gray-500 mt-0.5">
                  Mesa {detalle.mesaNumero} · El pedido permanece cerrado
                </p>
              )}
            </div>
            <button
              type="button"
              onClick={onClose}
              className="w-8 h-8 rounded-lg flex items-center justify-center text-gray-500 hover:text-gray-300 hover:bg-neutral-800 transition-colors"
              aria-label="Cerrar"
            >
              <X size={18} />
            </button>
          </div>

          {/* ── Body (scrollable) ── */}
          <div className="flex-1 overflow-y-auto px-5 py-4 space-y-5 scrollbar-thin">
            {isLoading && (
              <div className="flex items-center justify-center py-12">
                <Loader2 size={24} className="animate-spin text-gray-500" />
              </div>
            )}

            {isError && (
              <div className="text-center py-12 text-red-400 text-sm">
                Error al cargar el pedido. Intentá de nuevo.
              </div>
            )}

            {detalle && !isLoading && (
              <>
                {/* ── Sección: Ítems ── */}
                <section>
                  <h3 className="text-xs text-gray-500 uppercase tracking-wider font-medium mb-3">
                    Productos
                  </h3>
                  <div className="space-y-2">
                    {items.map((item) => (
                      <div
                        key={item.itemId}
                        className={[
                          'flex items-center gap-3 px-3 py-2.5 rounded-xl',
                          'bg-neutral-800/50 border border-neutral-800',
                          item.cantidad !== item.cantidadOriginal
                            ? 'border-amber-800/40'
                            : '',
                        ].join(' ')}
                      >
                        {/* Info */}
                        <div className="flex-1 min-w-0">
                          <p className="text-sm text-gray-200 truncate">{item.nombreProducto}</p>
                          <p className="text-xs text-gray-500 font-mono mt-0.5">
                            ${fmt(item.precioUnitario)} × {item.cantidad} ={' '}
                            <span className="text-gray-300">
                              ${fmt(item.precioUnitario * item.cantidad)}
                            </span>
                          </p>
                          {/* Badge de descuento */}
                          {item.montoDescuentoOriginal > 0 && (
                            <div className="flex items-center gap-1 mt-1">
                              <Tag size={10} className="text-emerald-500 shrink-0" />
                              <span className="text-[10px] text-emerald-400 truncate">
                                {item.descripcionDescuento ?? 'Descuento'}{' '}
                                <span className="font-mono">
                                  −${fmt(
                                    item.cantidadOriginal > 0
                                      ? (item.montoDescuentoOriginal / item.cantidadOriginal) * item.cantidad
                                      : item.montoDescuentoOriginal,
                                  )}
                                </span>
                              </span>
                            </div>
                          )}
                          {item.observacion && (
                            <p className="text-[10px] text-gray-600 mt-0.5 italic truncate">
                              {item.observacion}
                            </p>
                          )}
                        </div>

                        {/* Controles cantidad */}
                        <div className="flex items-center gap-1.5 shrink-0">
                          <button
                            type="button"
                            onClick={() => cambiarCantidad(item.itemId, -1)}
                            disabled={item.cantidad <= 1}
                            className={[
                              'w-8 h-8 rounded-lg flex items-center justify-center',
                              'border border-neutral-700 text-gray-400',
                              'hover:bg-neutral-700 hover:text-gray-200 transition-colors',
                              'disabled:opacity-30 disabled:cursor-not-allowed',
                            ].join(' ')}
                            aria-label="Reducir cantidad"
                          >
                            <Minus size={14} />
                          </button>
                          <span className="w-7 text-center text-sm font-mono font-semibold text-gray-200 tabular-nums">
                            {item.cantidad}
                          </span>
                          <button
                            type="button"
                            onClick={() => cambiarCantidad(item.itemId, 1)}
                            className={[
                              'w-8 h-8 rounded-lg flex items-center justify-center',
                              'border border-neutral-700 text-gray-400',
                              'hover:bg-neutral-700 hover:text-gray-200 transition-colors',
                            ].join(' ')}
                            aria-label="Aumentar cantidad"
                          >
                            <Plus size={14} />
                          </button>
                        </div>

                        {/* Eliminar */}
                        <button
                          type="button"
                          onClick={() => eliminarItem(item.itemId)}
                          className={[
                            'w-8 h-8 rounded-lg flex items-center justify-center shrink-0',
                            'text-gray-600 hover:text-red-400 hover:bg-red-950/30',
                            'transition-colors',
                          ].join(' ')}
                          aria-label={`Eliminar ${item.nombreProducto}`}
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    ))}
                  </div>

                  {/* Resumen productos */}
                  <div className="mt-3 px-3 space-y-1">
                    <div className="flex items-center justify-between">
                      <span className="text-xs text-gray-500">Subtotal productos</span>
                      <span className="text-sm font-mono text-gray-300 tabular-nums">
                        ${fmt(subtotalBruto)}
                      </span>
                    </div>
                    {hayDescuentos && (
                      <div className="flex items-center justify-between">
                        <span className="text-xs text-emerald-500/70">Descuentos</span>
                        <span className="text-sm font-mono text-emerald-400/80 tabular-nums">
                          −${fmt(totalDescuentos)}
                        </span>
                      </div>
                    )}
                    <div className="flex items-center justify-between pt-1 border-t border-neutral-800/50">
                      <span className="text-xs text-gray-400 font-medium">Total neto</span>
                      <span className="text-sm font-mono font-semibold text-gray-200 tabular-nums">
                        ${fmt(totalNeto)}
                      </span>
                    </div>
                  </div>
                </section>

                {/* ── Sección: Pagos ── */}
                <section>
                  <div className="flex items-center justify-between mb-3">
                    <h3 className="text-xs text-gray-500 uppercase tracking-wider font-medium">
                      Pagos
                    </h3>
                    <button
                      type="button"
                      onClick={agregarPago}
                      className="flex items-center gap-1 text-xs text-gray-500 hover:text-gray-300 transition-colors"
                    >
                      <PlusCircle size={13} />
                      Agregar pago
                    </button>
                  </div>

                  <div className="space-y-2">
                    {pagos.map((pago) => (
                      <div
                        key={pago.key}
                        className="flex items-center gap-2 px-3 py-2.5 rounded-xl bg-neutral-800/50 border border-neutral-800"
                      >
                        {/* Medio de pago */}
                        <select
                          value={pago.medio}
                          onChange={(e) =>
                            cambiarMedioPago(pago.key, e.target.value as MedioPago)
                          }
                          className={[
                            'flex-1 h-9 rounded-lg px-2 text-sm',
                            'bg-neutral-800 border border-neutral-700 text-gray-200',
                            'focus:outline-none focus:border-red-600/50',
                            'appearance-none cursor-pointer',
                          ].join(' ')}
                        >
                          {MEDIOS_PAGO.map((mp) => (
                            <option key={mp.value} value={mp.value}>
                              {mp.label}
                            </option>
                          ))}
                        </select>

                        {/* Monto */}
                        <div className="relative w-28">
                          <span className="absolute left-2.5 top-1/2 -translate-y-1/2 text-gray-500 text-sm">
                            $
                          </span>
                          <input
                            type="number"
                            value={pago.monto}
                            onChange={(e) => cambiarMontoPago(pago.key, e.target.value)}
                            min="0"
                            step="0.01"
                            className={[
                              'w-full h-9 rounded-lg pl-6 pr-2 text-sm font-mono text-right',
                              'bg-neutral-800 border border-neutral-700 text-gray-200',
                              'focus:outline-none focus:border-red-600/50',
                              '[appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none',
                            ].join(' ')}
                          />
                        </div>

                        {/* Eliminar pago */}
                        <button
                          type="button"
                          onClick={() => eliminarPago(pago.key)}
                          disabled={pagos.length <= 1}
                          className={[
                            'w-8 h-8 rounded-lg flex items-center justify-center shrink-0',
                            'text-gray-600 hover:text-red-400 hover:bg-red-950/30',
                            'transition-colors',
                            'disabled:opacity-20 disabled:cursor-not-allowed',
                          ].join(' ')}
                          aria-label="Eliminar pago"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    ))}
                  </div>

                  {/* Botón ajustar al total (solo si 1 pago) */}
                  {pagos.length === 1 && Math.abs(diferencia) > 0.01 && (
                    <button
                      type="button"
                      onClick={ajustarPagoAlTotal}
                      className="mt-2 w-full text-xs text-gray-500 hover:text-gray-300 text-center py-1.5 transition-colors"
                    >
                      Ajustar monto al total (${fmt(totalNeto)})
                    </button>
                  )}
                </section>
              </>
            )}
          </div>

          {/* ── Footer fijo ── */}
          {detalle && !isLoading && (
            <div className="px-5 py-4 border-t border-neutral-800 shrink-0 space-y-3">
              {/* Diferencia */}
              {Math.abs(diferencia) > 0.01 && (
                <div className="flex items-center gap-2 px-3 py-2 rounded-lg bg-amber-950/20 border border-amber-800/30">
                  <AlertTriangle size={14} className="text-amber-400 shrink-0" />
                  <p className="text-xs text-amber-300">
                    Diferencia de{' '}
                    <span className="font-mono font-semibold">
                      ${fmt(Math.abs(diferencia))}
                    </span>{' '}
                    — los pagos deben coincidir con el total
                  </p>
                </div>
              )}

              {/* Error */}
              {error && (
                <div className="flex items-center gap-2 px-3 py-2 rounded-lg bg-red-950/20 border border-red-800/30">
                  <AlertTriangle size={14} className="text-red-400 shrink-0" />
                  <p className="text-xs text-red-300">{error}</p>
                </div>
              )}

              {/* Resumen + botones */}
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-xs text-gray-500">Total corregido</p>
                  <p className="text-lg font-mono font-bold text-gray-100 tabular-nums">
                    ${fmt(totalNeto)}
                  </p>
                  {hayDescuentos && (
                    <p className="text-[10px] text-gray-600 mt-0.5">
                      Descuentos se recalculan al guardar
                    </p>
                  )}
                </div>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={onClose}
                    disabled={corregir.isPending}
                    className={[
                      'h-11 px-5 rounded-xl font-medium text-sm',
                      'bg-neutral-800 border border-neutral-700 text-gray-300',
                      'hover:bg-neutral-700 transition-colors active:scale-95',
                    ].join(' ')}
                  >
                    Cancelar
                  </button>
                  <button
                    type="button"
                    onClick={handleGuardar}
                    disabled={!esValido || !hayCambios || corregir.isPending}
                    className={[
                      'h-11 px-5 rounded-xl font-semibold text-sm',
                      'flex items-center gap-2 transition-all duration-150',
                      esValido && hayCambios && !corregir.isPending
                        ? 'bg-red-600 hover:bg-red-500 text-white active:scale-95'
                        : 'bg-neutral-700 text-gray-500 cursor-not-allowed',
                    ].join(' ')}
                  >
                    {corregir.isPending ? (
                      <>
                        <Loader2 size={16} className="animate-spin" />
                        Guardando...
                      </>
                    ) : (
                      <>
                        <Check size={16} />
                        Guardar corrección
                      </>
                    )}
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </>
  );
}
