import { useState, useCallback, useEffect, useMemo } from 'react';
import ListaCategorias from '../components/ListaCategorias';
import GrillaProductos from '../components/GrillaProductos';
import TicketPedido from '../components/TicketPedido';
import DescuentoManualModal from '../components/DescuentoManualModal';
import CerrarMesaModal from '../components/CerrarMesaModal';
import TicketPreviewModal from '../components/TicketPreviewModal';
import ConfigurarProductoModal from '../components/ConfigurarProductoModal';
import type { ConfigurarProductoPayload } from '../components/ConfigurarProductoModal';
import { useProductos } from '../../catalogo/hooks/useProductos';
import type { ProductoResponse } from '../../catalogo/types';
import { usePedidoMesa, useObtenerComanda } from '../../salon/hooks/useMesas';
import {
  useAgregarProducto,
  useModificarCantidad,
  useEliminarItem,
} from '../hooks/usePedido';
import useToast from '../../../hooks/useToast';

interface PantallaPedidoProps {
  mesaId: string;
  onCerrar: () => void;
}

/**
 * Modal overlay del POS / Terminal de Pedidos.
 *
 * Se renderiza encima del SalonPage como overlay al 90% del viewport.
 * Solo se cierra con el botón "Atrás" (no con click en backdrop ni Escape),
 * para evitar cierres accidentales durante la operación.
 *
 * Layout de 3 paneles:
 * - Izquierdo (15%): Categorías de productos (filtro por colorHex)
 * - Central (~55%): Grilla de productos con búsqueda typeahead
 * - Derecho (30%): Ticket del pedido con ítems, totales y acciones
 *
 * Flujo:
 * 1. Se abre desde SalonPage al hacer click en una mesa
 * 2. Si la mesa está ABIERTA → carga el pedido activo
 * 3. El operador selecciona productos → se agregan al pedido (mutation)
 * 4. El ticket se actualiza en tiempo real vía React Query invalidation
 */
export default function PantallaPedido({ mesaId, onCerrar }: PantallaPedidoProps) {
  const toast = useToast();

  // ── Estado local ──
  const [categoriaActiva, setCategoriaActiva] = useState<string | null>(null);
  const [busqueda, setBusqueda] = useState('');
  const [mostrarDescuento, setMostrarDescuento] = useState(false);
  const [mostrarCierre, setMostrarCierre] = useState(false);
  const [mostrarTicketPreview, setMostrarTicketPreview] = useState(false);
  /** Producto seleccionado para configurar (observaciones + extras) antes de agregar */
  const [productoSeleccionado, setProductoSeleccionado] = useState<ProductoResponse | null>(null);
  /** IDs de ítems ya enviados a cocina (tracking local por sesión) */
  const [itemsEnviadosIds, setItemsEnviadosIds] = useState<Set<string>>(new Set());

  // ── Datos del backend ──
  const { data: productos = [], isLoading: cargandoProductos } =
    useProductos(categoriaActiva);

  const { data: pedido = null, isLoading: cargandoPedido } =
    usePedidoMesa(mesaId);

  // ── Mutations ──
  const agregarProducto = useAgregarProducto();
  const modificarCantidad = useModificarCantidad();
  const eliminarItem = useEliminarItem();
  const obtenerComanda = useObtenerComanda();

  // ── Bloquear scroll del body mientras el modal está abierto ──
  useEffect(() => {
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = '';
    };
  }, []);

  // ── Filtrar productos por búsqueda typeahead ──
  const productosFiltrados = busqueda.trim()
    ? productos.filter((p) =>
        p.nombre.toLowerCase().includes(busqueda.trim().toLowerCase())
      )
    : productos;

  // ── Handlers ──

  /**
   * Al tocar un producto en la grilla, se abre el modal de configuración
   * en lugar de agregar directamente. Esto permite al operador definir
   * observaciones y extras antes de confirmar.
   */
  const handleAgregarProducto = useCallback(
    (productoId: string) => {
      if (!pedido?.pedidoId) {
        toast.error('No hay un pedido activo para esta mesa');
        return;
      }
      const producto = productos.find((p) => p.id === productoId);
      if (producto) {
        setProductoSeleccionado(producto);
      }
    },
    [pedido, productos, toast]
  );

  /**
   * Callback del modal de configuración.
   *
   * El frontend SIEMPRE delega al endpoint agregarProducto del backend,
   * que internamente maneja el merge: si ya existe un ítem con el mismo
   * productoId, acumula cantidad, combina extras y observaciones,
   * y recalcula promociones sobre la cantidad total.
   *
   * El frontend NO necesita detectar duplicados ni usar modificarCantidad
   * para este flujo. Esa responsabilidad pertenece al dominio (backend).
   */
  const handleConfirmarProducto = useCallback(
    (payload: ConfigurarProductoPayload) => {
      if (!pedido?.pedidoId) return;

      agregarProducto.mutate(
        {
          pedidoId: pedido.pedidoId,
          productoId: payload.productoId,
          cantidad: payload.cantidad,
          observaciones: payload.observaciones,
          extrasIds: payload.extrasIds.length > 0 ? payload.extrasIds : undefined,
        },
        {
          onSuccess: () => {
            setProductoSeleccionado(null);
          },
          onError: (error: any) => {
            const msg =
              error?.response?.data?.message || 'Error al agregar producto';
            toast.error(msg);
          },
        }
      );
    },
    [pedido, agregarProducto, toast]
  );

  const handleModificarCantidad = useCallback(
    (itemId: string, nuevaCantidad: number) => {
      if (!pedido?.pedidoId) return;

      modificarCantidad.mutate(
        { pedidoId: pedido.pedidoId, itemId, cantidad: nuevaCantidad },
        {
          onError: (error: any) => {
            const msg =
              error?.response?.data?.message || 'Error al modificar cantidad';
            toast.error(msg);
          },
        }
      );
    },
    [pedido, modificarCantidad, toast]
  );

  const handleEliminarItem = useCallback(
    (itemId: string) => {
      if (!pedido?.pedidoId) return;

      eliminarItem.mutate(
        { pedidoId: pedido.pedidoId, itemId },
        {
          onSuccess: () => {
            toast.success('Ítem eliminado');
          },
          onError: (error: any) => {
            const msg =
              error?.response?.data?.message || 'Error al eliminar ítem';
            toast.error(msg);
          },
        }
      );
    },
    [pedido, eliminarItem, toast]
  );

  const handleAplicarDescuento = useCallback(() => {
    if (!pedido?.pedidoId) return;
    setMostrarDescuento(true);
  }, [pedido]);

  const handleControlMesa = useCallback(() => {
    if (!pedido?.pedidoId) return;
    setMostrarTicketPreview(true);
  }, [pedido]);

  const handleCerrarMesa = useCallback(() => {
    if (!pedido?.pedidoId) return;
    setMostrarCierre(true);
  }, [pedido]);

  /**
   * Mandar a cocina: llama al endpoint de comanda y marca
   * los ítems actuales como "enviados" en el estado local.
   */
  const handleMandarCocina = useCallback(() => {
    if (!pedido?.pedidoId) return;

    obtenerComanda.mutate(mesaId, {
      onSuccess: () => {
        // Marcar todos los ítems actuales como enviados
        const idsActuales = new Set(pedido.items.map((i) => i.id));
        setItemsEnviadosIds((prev) => new Set([...prev, ...idsActuales]));
        toast.success('Comanda enviada a cocina');
      },
      onError: (error: any) => {
        const msg =
          error?.response?.data?.message || 'Error al enviar comanda';
        toast.error(msg);
      },
    });
  }, [pedido, mesaId, obtenerComanda, toast]);

  /**
   * Callback tras cierre exitoso de mesa:
   * cierra todos los modales y vuelve al salón.
   */
  const handleCierreExitoso = useCallback(() => {
    setMostrarCierre(false);
    onCerrar();
  }, [onCerrar]);

  // ── Memo: Set de IDs enviados (estable para prop del TicketPedido) ──
  const itemsEnviadosSet = useMemo(() => itemsEnviadosIds, [itemsEnviadosIds]);

  // ── Número de mesa para el header del ticket ──
  const numeroMesa = pedido?.numeroMesa ?? 0;

  return (
    <>
      {/* ── Backdrop oscuro (no cierra al click) ── */}
      <div
        className="fixed inset-0 z-[60] bg-black/70 backdrop-blur-sm animate-backdrop-in"
        aria-hidden="true"
      />

      {/* ── Modal Container ── */}
      <div
        className="
          fixed inset-0 z-[70]
          flex items-center justify-center
          p-4 sm:p-6 lg:p-8
          pointer-events-none
        "
        role="dialog"
        aria-modal="true"
        aria-label={`Pedido de Mesa ${numeroMesa}`}
      >
        <section
          className="
            pointer-events-auto
            w-full max-w-[95vw] h-[90vh]
            bg-neutral-950 rounded-2xl
            border border-neutral-800
            shadow-2xl shadow-black/60
            flex overflow-hidden
            animate-modal-in
          "
        >
          {/* ── Panel Izquierdo: Categorías (15%) ── */}
          <aside className="w-[15%] min-w-[130px] border-r border-neutral-800 bg-neutral-950 overflow-y-auto shrink-0">
            <ListaCategorias
              categoriaActiva={categoriaActiva}
              onCategoriaChange={(color) => {
                setCategoriaActiva(color);
                setBusqueda('');
              }}
            />
          </aside>

          {/* ── Panel Central: Grilla de Productos (~55%) ── */}
          <main className="flex-1 overflow-y-auto bg-neutral-900/30 flex flex-col">
            <GrillaProductos
              productos={productosFiltrados}
              cargando={cargandoProductos}
              onAgregarProducto={handleAgregarProducto}
              busqueda={busqueda}
              onBusquedaChange={setBusqueda}
              totalProductos={productos.length}
            />
          </main>

          {/* ── Panel Derecho: Ticket del Pedido (30%) ── */}
          <aside className="w-[30%] min-w-[300px] shrink-0">
            <TicketPedido
              pedido={pedido}
              cargando={cargandoPedido}
              numeroMesa={numeroMesa}
              onVolver={onCerrar}
              onModificarCantidad={handleModificarCantidad}
              onEliminarItem={handleEliminarItem}
              onAplicarDescuento={handleAplicarDescuento}
              onCerrarMesa={handleCerrarMesa}
              onControlMesa={handleControlMesa}
              onMandarCocina={handleMandarCocina}
              enviandoCocina={obtenerComanda.isPending}
              itemsEnviadosIds={itemsEnviadosSet}
            />
          </aside>
        </section>
      </div>

      {/* ── Modal: Configurar Producto (Observaciones + Extras) ── */}
      {productoSeleccionado && (
        <ConfigurarProductoModal
          producto={productoSeleccionado}
          onConfirmar={handleConfirmarProducto}
          onCerrar={() => setProductoSeleccionado(null)}
          enviando={agregarProducto.isPending}
        />
      )}

      {/* ── Modal: Descuento Manual ── */}
      {mostrarDescuento && pedido && (
        <DescuentoManualModal
          pedido={pedido}
          onClose={() => setMostrarDescuento(false)}
        />
      )}

      {/* ── Modal: Control de Mesa (Preview del ticket) ── */}
      {mostrarTicketPreview && pedido && (
        <TicketPreviewModal
          mesaId={mesaId}
          onClose={() => setMostrarTicketPreview(false)}
        />
      )}

      {/* ── Modal: Cierre de Mesa y Pago ── */}
      {mostrarCierre && pedido && (
        <CerrarMesaModal
          mesaId={mesaId}
          pedido={pedido}
          onClose={() => setMostrarCierre(false)}
          onSuccess={handleCierreExitoso}
        />
      )}
    </>
  );
}
