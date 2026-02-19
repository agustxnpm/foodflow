import { X, Clock, ShoppingBag, Tag, UtensilsCrossed } from 'lucide-react';
import type { Mesa } from '../types';
import type { DetallePedidoResponse } from '../../pedido/types';

interface PedidoDetalleModalProps {
  mesa: Mesa | null;
  pedido: DetallePedidoResponse | null;
  cargando: boolean;
  onClose: () => void;
  onAbrirMesa?: (mesaId: string) => void;
  abriendoMesa?: boolean;
}

/* ──────────────────────────────────── */
/*  Skeleton Loading                    */
/* ──────────────────────────────────── */

/**
 * Barra pulsante individual para skeleton loading.
 * Se compone para simular la estructura del contenido real.
 */
function SkeletonBar({ className = '' }: { className?: string }) {
  return (
    <div
      className={`bg-neutral-700 rounded-md animate-pulse ${className}`}
    />
  );
}

/**
 * Skeleton que simula la estructura del detalle de un pedido:
 * header, lista de ítems y sección de totales.
 *
 * DECISIÓN UX: Skeleton > Spinner porque reproduce la estructura
 * visual del contenido final, reduciendo la percepción de espera.
 */
function SkeletonPedido() {
  return (
    <div className="space-y-6 p-6" aria-busy="true" aria-label="Cargando pedido">
      {/* Header skeleton */}
      <div className="space-y-3">
        <SkeletonBar className="h-7 w-52" />
        <SkeletonBar className="h-4 w-36" />
      </div>

      <div className="border-t border-neutral-700" />

      {/* Items skeleton */}
      <div className="space-y-5">
        {[1, 2, 3].map((i) => (
          <div key={i} className="flex justify-between items-start">
            <div className="space-y-2 flex-1">
              <SkeletonBar className="h-5 w-3/4" />
              <SkeletonBar className="h-3 w-1/2" />
            </div>
            <SkeletonBar className="h-6 w-20 ml-4" />
          </div>
        ))}
      </div>

      <div className="border-t border-neutral-700" />

      {/* Totals skeleton */}
      <div className="space-y-3">
        <div className="flex justify-between">
          <SkeletonBar className="h-5 w-24" />
          <SkeletonBar className="h-5 w-20" />
        </div>
        <div className="flex justify-between">
          <SkeletonBar className="h-5 w-28" />
          <SkeletonBar className="h-5 w-16" />
        </div>
        <div className="border-t border-neutral-700 pt-3 flex justify-between">
          <SkeletonBar className="h-8 w-20" />
          <SkeletonBar className="h-8 w-28" />
        </div>
      </div>
    </div>
  );
}

/* ──────────────────────────────────── */
/*  Contenido real del pedido           */
/* ──────────────────────────────────── */

function PedidoContenido({ pedido }: { pedido: DetallePedidoResponse }) {
  const formatHora = (fecha: string) => {
    const d = new Date(fecha);
    return d.toLocaleTimeString('es-AR', {
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const hayDescuentos = pedido.totalDescuentos > 0;

  return (
    <div className="p-6 space-y-5">
      {/* Header */}
      <div>
        <h2 className="text-2xl font-bold text-gray-100">
          Mesa {pedido.numeroMesa}
        </h2>
        <div className="flex items-center gap-2 mt-1 text-sm text-gray-400">
          <Clock size={14} />
          <span>Abierto desde {formatHora(pedido.fechaApertura)}</span>
          <span className="text-neutral-600">·</span>
          <span className="text-red-400 font-semibold">
            Pedido #{pedido.numeroPedido}
          </span>
        </div>
      </div>

      <div className="border-t border-neutral-700" />

      {/* Lista de ítems */}
      <div className="space-y-4">
        <div className="flex items-center gap-2 text-sm text-gray-500">
          <ShoppingBag size={14} />
          <span className="font-semibold uppercase tracking-wider text-[11px]">
            Ítems del pedido
          </span>
        </div>

        {pedido.items.map((item) => (
          <div
            key={item.id}
            className="flex justify-between items-start py-2"
          >
            <div className="flex-1 min-w-0">
              <p className="text-gray-100 font-medium">
                <span className="text-red-400 font-mono mr-1.5">
                  {item.cantidad}x
                </span>
                {item.nombreProducto}
              </p>
              {item.observacion && (
                <p className="text-xs text-gray-500 mt-0.5 italic">
                  &ldquo;{item.observacion}&rdquo;
                </p>
              )}
              {item.tienePromocion && item.nombrePromocion && (
                <div className="flex items-center gap-1 mt-1">
                  <Tag size={10} className="text-green-400" />
                  <span className="text-xs text-green-400">
                    {item.nombrePromocion} (-$ {item.descuentoTotal.toLocaleString('es-AR')})
                  </span>
                </div>
              )}
            </div>
            <div className="ml-4 text-right whitespace-nowrap">
              {item.descuentoTotal > 0 ? (
                <>
                  <span className="text-xs text-gray-500 line-through font-mono block">
                    $ {item.subtotal.toLocaleString('es-AR')}
                  </span>
                  <span className="text-gray-200 font-mono font-semibold">
                    $ {item.precioFinal.toLocaleString('es-AR')}
                  </span>
                </>
              ) : (
                <span className="text-gray-300 font-mono font-semibold">
                  $ {item.subtotal.toLocaleString('es-AR')}
                </span>
              )}
            </div>
          </div>
        ))}
      </div>

      <div className="border-t border-neutral-700" />

      {/* Totales */}
      <div className="space-y-2">
        <div className="flex justify-between text-gray-400">
          <span>Subtotal</span>
          <span className="font-mono">
            $ {pedido.subtotal.toLocaleString('es-AR')}
          </span>
        </div>
        {hayDescuentos && (
          <div className="flex justify-between text-green-400 text-sm">
            <span>Descuentos</span>
            <span className="font-mono">
              -$ {pedido.totalDescuentos.toLocaleString('es-AR')}
            </span>
          </div>
        )}
        <div className="flex justify-between items-baseline pt-3 border-t border-neutral-700">
          <span className="text-lg font-bold text-gray-100">Total</span>
          <span className="text-2xl font-bold text-red-500 font-mono">
            $ {pedido.totalParcial.toLocaleString('es-AR')}
          </span>
        </div>
      </div>
    </div>
  );
}

/* ──────────────────────────────────── */
/*  Modal principal                     */
/* ──────────────────────────────────── */

/**
 * Modal de detalle del pedido de una mesa.
 *
 * DECISIÓN UX: Usa Skeleton Loading (barras pulsantes) en lugar de spinners,
 * para dar feedback inmediato de la estructura del contenido que se va a mostrar.
 * Esto reduce la percepción de espera del operador durante la consulta.
 */
/**
 * Contenido para mesa en estado LIBRE.
 * Muestra una acción clara para abrir la mesa e iniciar un pedido.
 */
function MesaLibreContenido({
  mesa,
  onAbrirMesa,
  abriendoMesa = false,
}: {
  mesa: Mesa;
  onAbrirMesa: (mesaId: string) => void;
  abriendoMesa?: boolean;
}) {
  return (
    <div className="p-6 flex flex-col items-center text-center space-y-6">
      <div className="w-16 h-16 rounded-full bg-neutral-800 flex items-center justify-center">
        <UtensilsCrossed size={32} className="text-gray-500" />
      </div>

      <div className="space-y-1">
        <h2 className="text-2xl font-bold text-gray-100">
          Mesa {mesa.numero}
        </h2>
        <p className="text-sm text-gray-500">
          Esta mesa está libre. Abrila para comenzar a cargar un pedido.
        </p>
      </div>

      <button
        type="button"
        onClick={() => onAbrirMesa(mesa.id)}
        disabled={abriendoMesa}
        className="
          w-full h-14
          bg-red-600 text-white
          rounded-xl font-bold text-lg
          hover:bg-red-700
          disabled:bg-neutral-700 disabled:text-gray-500 disabled:cursor-not-allowed
          transition-colors active:scale-95
        "
      >
        {abriendoMesa ? 'Abriendo mesa…' : 'Abrir Mesa'}
      </button>
    </div>
  );
}

/**
 * Modal dual de mesa.
 *
 * - Mesa LIBRE: muestra acción para abrir pedido.
 * - Mesa ABIERTA: muestra skeleton loading → detalle del pedido.
 */
export default function PedidoDetalleModal({
  mesa,
  pedido,
  cargando,
  onClose,
  onAbrirMesa,
  abriendoMesa = false,
}: PedidoDetalleModalProps) {
  if (!mesa) return null;

  const isLibre = mesa.estado === 'LIBRE';

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-label={`Mesa ${mesa.numero} — ${isLibre ? 'Libre' : 'Abierta'}`}
    >
      <div
        className="
          relative
          bg-neutral-900
          border border-neutral-700
          rounded-2xl
          w-full max-w-lg
          max-h-[85vh]
          overflow-y-auto
          shadow-2xl shadow-black/50
          animate-fadeIn
        "
        onClick={(e) => e.stopPropagation()}
      >
        {/* Botón cerrar */}
        <button
          type="button"
          onClick={onClose}
          className="
            absolute top-4 right-4 z-10
            w-10 h-10 rounded-full
            bg-neutral-800 text-gray-400
            flex items-center justify-center
            hover:bg-neutral-700 hover:text-gray-200
            transition-colors
          "
          aria-label="Cerrar"
        >
          <X size={20} />
        </button>

        {/* ── Contenido según estado de la mesa ── */}
        {isLibre && onAbrirMesa ? (
          <MesaLibreContenido
            mesa={mesa}
            onAbrirMesa={onAbrirMesa}
            abriendoMesa={abriendoMesa}
          />
        ) : cargando ? (
          <SkeletonPedido />
        ) : pedido ? (
          <PedidoContenido pedido={pedido} />
        ) : (
          <div className="p-6 text-center text-gray-500">
            No se encontró información del pedido.
          </div>
        )}
      </div>
    </div>
  );
}
