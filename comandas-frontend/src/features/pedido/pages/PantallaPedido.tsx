import { useState, useCallback, useEffect } from 'react';
import ListaCategorias from '../components/ListaCategorias';
import GrillaProductos from '../components/GrillaProductos';
import TicketPedido from '../components/TicketPedido';
import { useProductos } from '../../catalogo/hooks/useProductos';
import { usePedidoMesa } from '../../salon/hooks/useMesas';
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

  // ── Datos del backend ──
  const { data: productos = [], isLoading: cargandoProductos } =
    useProductos(categoriaActiva);

  const { data: pedido = null, isLoading: cargandoPedido } =
    usePedidoMesa(mesaId);

  // ── Mutations ──
  const agregarProducto = useAgregarProducto();
  const modificarCantidad = useModificarCantidad();
  const eliminarItem = useEliminarItem();

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

  const handleAgregarProducto = useCallback(
    (productoId: string) => {
      if (!pedido?.pedidoId) {
        toast.error('No hay un pedido activo para esta mesa');
        return;
      }

      // Buscar si el producto ya existe en el pedido para sumar cantidad
      const producto = productos.find((p) => p.id === productoId);
      const itemExistente = producto
        ? pedido.items?.find((i) => i.nombreProducto === producto.nombre)
        : undefined;

      if (itemExistente) {
        modificarCantidad.mutate(
          {
            pedidoId: pedido.pedidoId,
            itemId: itemExistente.id,
            cantidad: itemExistente.cantidad + 1,
          },
          {
            onError: (error: any) => {
              const msg =
                error?.response?.data?.message || 'Error al modificar cantidad';
              toast.error(msg);
            },
          }
        );
      } else {
        agregarProducto.mutate(
          { pedidoId: pedido.pedidoId, productoId, cantidad: 1 },
          {
            onError: (error: any) => {
              const msg =
                error?.response?.data?.message || 'Error al agregar producto';
              toast.error(msg);
            },
          }
        );
      }
    },
    [pedido, productos, agregarProducto, modificarCantidad, toast]
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
    toast.error('Descuentos manuales — próximamente');
  }, [toast]);

  const handleCerrarMesa = useCallback(() => {
    toast.error('Cierre de mesa — próximamente');
  }, [toast]);

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
            />
          </aside>
        </section>
      </div>
    </>
  );
}
