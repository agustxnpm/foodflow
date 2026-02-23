import {
  ArrowLeft,
  Clock,
  ShoppingBag,
  Tag,
  Percent,
  CreditCard,
  Trash2,
  Minus,
  Plus,
  ChefHat,
  Loader2,
  Receipt,
} from 'lucide-react';
import type { DetallePedidoResponse, ItemDetalle } from '../types';

// ─── Skeleton ─────────────────────────────────────────────────────────────────

function SkeletonBar({ className = '' }: { className?: string }) {
  return (
    <div className={`bg-neutral-700 rounded-md animate-pulse ${className}`} />
  );
}

function TicketSkeleton() {
  return (
    <div className="p-4 space-y-4" aria-busy="true" aria-label="Cargando pedido">
      <div className="space-y-2">
        <SkeletonBar className="h-6 w-40" />
        <SkeletonBar className="h-3 w-28" />
      </div>
      <div className="border-t border-neutral-800" />
      {[1, 2, 3].map((i) => (
        <div key={i} className="flex justify-between items-center py-2">
          <div className="space-y-1.5 flex-1">
            <SkeletonBar className="h-4 w-3/4" />
            <SkeletonBar className="h-3 w-1/3" />
          </div>
          <SkeletonBar className="h-5 w-16 ml-3" />
        </div>
      ))}
      <div className="border-t border-neutral-800" />
      <div className="space-y-2">
        <div className="flex justify-between">
          <SkeletonBar className="h-4 w-20" />
          <SkeletonBar className="h-4 w-16" />
        </div>
        <div className="flex justify-between">
          <SkeletonBar className="h-7 w-16" />
          <SkeletonBar className="h-7 w-24" />
        </div>
      </div>
    </div>
  );
}

// ─── Ítem del ticket ──────────────────────────────────────────────────────────

interface TicketItemProps {
  item: ItemDetalle;
  onModificarCantidad: (itemId: string, nuevaCantidad: number) => void;
  onEliminar: (itemId: string) => void;
  /** Si el ítem ya fue enviado a cocina */
  enviadoACocina?: boolean;
}

/**
 * Línea individual de un ítem en el ticket.
 *
 * Replica el diseño del modal oscuro anterior:
 * - Cantidad en rojo (ej: "2x")
 * - Nombre del producto
 * - Precio (tachado si hay descuento)
 * - Si hay promo: etiqueta verde con el nombre y ahorro
 * - Controles +/- para modificar cantidad
 * - Botón eliminar
 */
function TicketItem({ item, onModificarCantidad, onEliminar, enviadoACocina = false }: TicketItemProps) {
  const hayDescuento = item.descuentoTotal > 0;

  return (
    <div className="group py-3 border-b border-neutral-800/60 last:border-b-0">
      <div className="flex items-start justify-between gap-2">
        {/* Info del ítem */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-1.5">
            <p className={[
              'text-sm font-medium leading-tight',
              enviadoACocina ? 'text-gray-400' : 'text-gray-200',
            ].join(' ')}>
              <span className={[
                'font-mono font-bold mr-1.5',
                enviadoACocina ? 'text-gray-500' : 'text-red-400',
              ].join(' ')}>
                {item.cantidad}x
              </span>
              {item.nombreProducto}
            </p>
            {/* Badge de estado cocina */}
            {enviadoACocina ? (
              <span className="shrink-0 text-[9px] font-bold uppercase tracking-wider px-1.5 py-0.5 rounded-full bg-neutral-800 text-gray-500 border border-neutral-700">
                Enviado
              </span>
            ) : (
              <span className="shrink-0 text-[9px] font-bold uppercase tracking-wider px-1.5 py-0.5 rounded-full bg-yellow-900/30 text-yellow-400 border border-yellow-700/40">
                Nuevo
              </span>
            )}
          </div>

          {item.observacion && (
            <p className="text-[11px] text-gray-500 mt-0.5 italic truncate">
              &ldquo;{item.observacion}&rdquo;
            </p>
          )}

          {item.tienePromocion && item.nombrePromocion && (
            <div className="flex items-center gap-1 mt-1">
              <Tag size={10} className="text-green-400 shrink-0" />
              <span className="text-[11px] text-green-400 truncate">
                {item.nombrePromocion} (-$ {item.descuentoTotal.toLocaleString('es-AR')})
              </span>
            </div>
          )}
        </div>

        {/* Precio */}
        <div className="text-right whitespace-nowrap shrink-0">
          {hayDescuento ? (
            <>
              <span className="text-[11px] text-gray-600 line-through font-mono block">
                $ {item.subtotal.toLocaleString('es-AR')}
              </span>
              <span className="text-sm text-gray-100 font-mono font-bold">
                $ {item.precioFinal.toLocaleString('es-AR')}
              </span>
            </>
          ) : (
            <span className="text-sm text-gray-200 font-mono font-bold">
              $ {item.subtotal.toLocaleString('es-AR')}
            </span>
          )}
        </div>
      </div>

      {/* Controles de cantidad + eliminar */}
      <div className="flex items-center justify-between mt-2">
        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={() =>
              item.cantidad <= 1
                ? onEliminar(item.id)
                : onModificarCantidad(item.id, item.cantidad - 1)
            }
            className="
              w-7 h-7 rounded-md
              flex items-center justify-center
              bg-neutral-800 text-gray-500
              hover:bg-neutral-700 hover:text-gray-300
              transition-colors
            "
            aria-label="Reducir cantidad"
          >
            <Minus size={14} />
          </button>

          <span className="w-8 text-center text-sm font-bold text-gray-300 tabular-nums">
            {item.cantidad}
          </span>

          <button
            type="button"
            onClick={() => onModificarCantidad(item.id, item.cantidad + 1)}
            className="
              w-7 h-7 rounded-md
              flex items-center justify-center
              bg-neutral-800 text-gray-500
              hover:bg-neutral-700 hover:text-gray-300
              transition-colors
            "
            aria-label="Aumentar cantidad"
          >
            <Plus size={14} />
          </button>
        </div>

        <button
          type="button"
          onClick={() => onEliminar(item.id)}
          className="
            w-7 h-7 rounded-md
            flex items-center justify-center
            text-gray-600
            hover:bg-red-950/50 hover:text-red-400
            transition-colors
          "
          aria-label={`Eliminar ${item.nombreProducto}`}
        >
          <Trash2 size={14} />
        </button>
      </div>
    </div>
  );
}

// ─── Ticket Principal ─────────────────────────────────────────────────────────

interface TicketPedidoProps {
  pedido: DetallePedidoResponse | null;
  cargando: boolean;
  numeroMesa: number;
  onVolver: () => void;
  onModificarCantidad: (itemId: string, nuevaCantidad: number) => void;
  onEliminarItem: (itemId: string) => void;
  onAplicarDescuento: () => void;
  onCerrarMesa: () => void;
  onControlMesa: () => void;
  onMandarCocina: () => void;
  enviandoCocina: boolean;
  /** IDs de ítems que ya fueron enviados a cocina */
  itemsEnviadosIds: Set<string>;
}

/**
 * Panel derecho del POS: ticket/resumen del pedido activo.
 *
 * Estructura (réplica del diseño del modal oscuro):
 * - Header: Mesa X — Pedido #N + botón volver
 * - Lista de ítems con descuentos (scrollable)
 * - Sección de totales: subtotal, descuentos (verde), total (rojo)
 * - Botonera de acciones: descuento manual + cerrar mesa
 */
export default function TicketPedido({
  pedido,
  cargando,
  numeroMesa,
  onVolver,
  onModificarCantidad,
  onEliminarItem,
  onAplicarDescuento,
  onCerrarMesa,
  onControlMesa,
  onMandarCocina,
  enviandoCocina,
  itemsEnviadosIds,
}: TicketPedidoProps) {
  const hayItems = pedido && pedido.items.length > 0;
  const hayDescuentos = pedido && pedido.totalDescuentos > 0;
  const hayItemsNuevos = pedido
    ? pedido.items.some((i) => !itemsEnviadosIds.has(i.id))
    : false;

  const formatHora = (fecha: string) => {
    const d = new Date(fecha);
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  };

  return (
    <div className="flex flex-col h-full bg-neutral-950 border-l border-neutral-800">
      {/* ── Header ── */}
      <div className="px-4 py-4 border-b border-neutral-800 shrink-0">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-lg font-bold text-gray-100">
              Mesa {numeroMesa}
            </h2>
            {pedido && (
              <div className="flex items-center gap-2 mt-0.5 text-xs text-gray-500">
                <Clock size={12} />
                <span>Desde {formatHora(pedido.fechaApertura)}</span>
                <span className="text-neutral-700">·</span>
                <span className="text-red-400 font-semibold">
                  Pedido #{pedido.numeroPedido}
                </span>
              </div>
            )}
          </div>

          <button
            type="button"
            onClick={onVolver}
            className="
              flex items-center gap-1.5
              px-3 py-2 rounded-lg
              text-sm font-semibold text-gray-400
              bg-neutral-800/80 border border-neutral-700
              hover:border-neutral-600 hover:text-gray-300
              transition-colors active:scale-95
            "
          >
            <ArrowLeft size={16} />
            <span>Atrás</span>
          </button>
        </div>
      </div>

      {/* ── Contenido: Skeleton / Vacío / Lista de ítems ── */}
      <div className="flex-1 overflow-y-auto">
        {cargando ? (
          <TicketSkeleton />
        ) : !hayItems ? (
          <div className="flex flex-col items-center justify-center h-full p-6 gap-3">
            <div className="w-14 h-14 rounded-full bg-neutral-800 flex items-center justify-center">
              <ShoppingBag size={24} className="text-gray-600" />
            </div>
            <p className="text-gray-600 text-sm text-center leading-relaxed">
              Pedido vacío
            </p>
            <p className="text-gray-700 text-xs text-center leading-relaxed max-w-[200px]">
              Buscá un producto o seleccionalo de la grilla para empezar
            </p>
          </div>
        ) : (
          <div className="px-4">
            {/* Label de sección */}
            <div className="flex items-center gap-2 pt-3 pb-1 text-gray-600">
              <ShoppingBag size={13} />
              <span className="text-[10px] font-bold uppercase tracking-widest">
                Ítems ({pedido!.items.length})
              </span>
            </div>

            {/* Lista de ítems */}
            {pedido!.items.map((item) => (
              <TicketItem
                key={item.id}
                item={item}
                onModificarCantidad={onModificarCantidad}
                onEliminar={onEliminarItem}
                enviadoACocina={itemsEnviadosIds.has(item.id)}
              />
            ))}

            {/* Hint sutil de guía */}
            <p className="text-[10px] text-gray-700 text-center py-3 leading-relaxed">
              Usá +/- para ajustar cantidades · Deslizá abajo para cobrar
            </p>
          </div>
        )}
      </div>

      {/* ── Totales ── */}
      {pedido && hayItems && (
        <div className="px-4 py-3 border-t border-neutral-800 space-y-1.5 shrink-0 bg-neutral-900/50">
          <div className="flex justify-between text-sm text-gray-500">
            <span>Subtotal</span>
            <span className="font-mono tabular-nums">
              $ {pedido.subtotal.toLocaleString('es-AR')}
            </span>
          </div>

          {hayDescuentos && (
            <div className="flex justify-between text-sm text-green-400">
              <span>Descuentos</span>
              <span className="font-mono tabular-nums">
                -$ {pedido.totalDescuentos.toLocaleString('es-AR')}
              </span>
            </div>
          )}

          <div className="flex justify-between items-baseline pt-2 border-t border-neutral-800">
            <span className="text-base font-bold text-gray-200">Total</span>
            <span className="text-2xl font-bold text-red-500 font-mono tabular-nums">
              $ {pedido.totalParcial.toLocaleString('es-AR')}
            </span>
          </div>
        </div>
      )}

      {/* ── Botonera de acciones ── */}
      <div className="px-4 py-3 border-t border-neutral-800 shrink-0 space-y-2">
        {/* Botón Mandar a Cocina */}
        <button
          type="button"
          onClick={onMandarCocina}
          disabled={!hayItemsNuevos || enviandoCocina}
          className="
            w-full flex items-center justify-center gap-2
            h-11 rounded-xl
            text-sm font-semibold
            bg-orange-900/30 text-orange-300
            border border-orange-700/40
            hover:bg-orange-900/50 hover:text-orange-200
            disabled:opacity-40 disabled:cursor-not-allowed
            transition-colors active:scale-[0.98]
          "
        >
          {enviandoCocina ? (
            <>
              <Loader2 size={16} className="animate-spin" />
              <span>Enviando…</span>
            </>
          ) : (
            <>
              <ChefHat size={16} />
              <span>Mandar a Cocina</span>
            </>
          )}
        </button>

        <button
          type="button"
          onClick={onAplicarDescuento}
          disabled={!hayItems}
          className="
            w-full flex items-center justify-center gap-2
            h-11 rounded-xl
            text-sm font-semibold
            bg-neutral-800 text-gray-300
            border border-neutral-700
            hover:border-neutral-600 hover:text-gray-100
            disabled:opacity-40 disabled:cursor-not-allowed
            transition-colors active:scale-[0.98]
          "
        >
          <Percent size={16} />
          <span>Descuento Manual</span>
        </button>

        <button
          type="button"
          onClick={onControlMesa}
          disabled={!hayItems}
          className="
            w-full flex items-center justify-center gap-2
            h-11 rounded-xl
            text-sm font-semibold
            bg-neutral-800 text-gray-300
            border border-neutral-700
            hover:border-neutral-600 hover:text-gray-100
            disabled:opacity-40 disabled:cursor-not-allowed
            transition-colors active:scale-[0.98]
          "
        >
          <Receipt size={16} />
          <span>Control de Mesa</span>
        </button>

        <button
          type="button"
          onClick={onCerrarMesa}
          disabled={!hayItems}
          className="
            w-full flex items-center justify-center gap-2
            h-12 rounded-xl
            text-base font-bold
            bg-red-600 text-white
            hover:bg-red-500
            disabled:bg-neutral-700 disabled:text-gray-500 disabled:cursor-not-allowed
            transition-colors active:scale-[0.98]
            shadow-sm shadow-red-950/40
          "
        >
          <CreditCard size={18} />
          <span>Cerrar Mesa / Cobrar</span>
        </button>
      </div>
    </div>
  );
}
