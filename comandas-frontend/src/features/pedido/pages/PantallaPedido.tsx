import { useState, useCallback, useEffect, useMemo } from 'react';
import ListaCategorias from '../components/ListaCategorias';
import GrillaProductos from '../components/GrillaProductos';
import TicketPedido from '../components/TicketPedido';
import DescuentoManualModal from '../components/DescuentoManualModal';
import CerrarMesaModal from '../components/CerrarMesaModal';
import TicketPreviewModal from '../components/TicketPreviewModal';
import ConfigurarProductoModal from '../components/ConfigurarProductoModal';
import type { ConfigurarProductoPayload } from '../components/ConfigurarProductoModal';
import VarianteSelectorModal from '../components/VarianteSelectorModal';
import { useProductos } from '../../catalogo/hooks/useProductos';
import type { ProductoResponse } from '../../catalogo/types';
import { usePedidoMesa, useObtenerComanda } from '../../salon/hooks/useMesas';
import {
  useAgregarProducto,
  useModificarCantidad,
  useEliminarItem,
} from '../hooks/usePedido';
import { permiteAbrirModal } from '../utils/productoUtils';
import { useCategorias } from '../../categorias/hooks/useCategorias';
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

  // ── Estado de variantes ──
  /** Producto base del grupo de variantes que abrió el selector */
  const [varianteSelectorBase, setVarianteSelectorBase] = useState<ProductoResponse | null>(null);
  /** Lista de variantes del grupo seleccionado */
  const [variantesDelGrupo, setVariantesDelGrupo] = useState<ProductoResponse[]>([]);
  /** ID de la variante seleccionada (se propaga al ConfigurarProductoModal y al request) */
  const [varianteSeleccionadaId, setVarianteSeleccionadaId] = useState<string | undefined>(undefined);

  // ── Datos del backend ──
  const { data: productos = [], isLoading: cargandoProductos } =
    useProductos(categoriaActiva, true);

  const { data: pedido = null, isLoading: cargandoPedido } =
    usePedidoMesa(mesaId);

  // ── Categorías del backend (para resolver flags de comportamiento) ──
  const { data: categorias = [] } = useCategorias();

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

  // ── Filtrar productos por búsqueda typeahead y excluir extras ──
  // Los extras (huevo, queso, disco) no se muestran en la grilla del POS
  // porque no pueden agregarse como líneas independientes. Solo se seleccionan
  // desde el modal de configuración de otro producto.
  const productosFiltrados = useMemo(() => {
    const sinExtras = productos.filter((p) => !p.esExtra);
    if (!busqueda.trim()) return sinExtras;
    const termino = busqueda.trim().toLowerCase();
    return sinExtras.filter((p) => p.nombre.toLowerCase().includes(termino));
  }, [productos, busqueda]);

  // Total de productos sin extras (para el indicador "N de M" en la grilla)
  const totalProductosSinExtras = useMemo(
    () => productos.filter((p) => !p.esExtra).length,
    [productos]
  );

  /**
   * Al tocar un producto en la grilla:
   *
   * 1. Si el producto pertenece a un grupo de variantes (grupoVarianteId != null)
   *    → abre VarianteSelectorModal para elegir la variante específica
   * 2. Si permiteAbrirModal() = true → abre modal de observaciones/extras
   * 3. Si permiteAbrirModal() = false → agrega directamente al pedido (cantidad 1, sin extras)
   *
   * La decisión integra: requiereConfiguracion, permiteExtras, esExtra,
   * categoría resuelta, y grupoVarianteId para variantes.
   */
  const handleAgregarProducto = useCallback(
    (productoId: string) => {
      if (!pedido?.pedidoId) {
        toast.error('No hay un pedido activo para esta mesa');
        return;
      }
      const producto = productos.find((p) => p.id === productoId);
      if (!producto) return;

      // Si tiene variantes → abrir selector de variantes primero
      if (producto.grupoVarianteId) {
        const hermanas = productos.filter(
          (p) => p.grupoVarianteId === producto.grupoVarianteId
        );
        if (hermanas.length > 1) {
          setVarianteSelectorBase(producto);
          setVariantesDelGrupo(hermanas);
          return;
        }
        // Si solo queda 1 variante visible (ej: filtro de búsqueda), actúa como producto normal
      }

      // Sin variantes: flujo clásico
      if (permiteAbrirModal(producto, categorias)) {
        setVarianteSeleccionadaId(undefined);
        setProductoSeleccionado(producto);
      } else {
        agregarProducto.mutate(
          {
            pedidoId: pedido.pedidoId,
            productoId: producto.id,
            cantidad: 1,
          },
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
    [pedido, productos, toast, agregarProducto, categorias]
  );

  /**
   * Callback del selector de variantes.
   *
   * Cuando el operador elige una variante del grupo, se evalúa si
   * esa variante necesita configuración (observaciones + extras) o
   * se agrega directamente al pedido.
   *
   * El varianteId se propaga al backend para selección explícita
   * (sin auto-normalización por discos).
   */
  const handleVarianteSeleccionada = useCallback(
    (variante: ProductoResponse) => {
      if (!pedido?.pedidoId) return;

      // Cerrar el selector de variantes
      setVarianteSelectorBase(null);
      setVariantesDelGrupo([]);

      if (permiteAbrirModal(variante, categorias)) {
        // Abrir ConfigurarProductoModal con la variante seleccionada
        setVarianteSeleccionadaId(variante.id);
        setProductoSeleccionado(variante);
      } else {
        // Agregar directamente con varianteId explícito
        agregarProducto.mutate(
          {
            pedidoId: pedido.pedidoId,
            productoId: variante.id,
            cantidad: 1,
            varianteId: variante.id,
          },
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
    [pedido, categorias, agregarProducto, toast]
  );

  /**
   * Callback del modal de configuración.
   *
   * El frontend SIEMPRE delega al endpoint agregarProducto del backend,
   * que internamente maneja el merge: si ya existe un ítem con el mismo
   * productoId, acumula cantidad, combina extras y observaciones,
   * y recalcula promociones sobre la cantidad total.
   *
   * Si hay varianteId, se envía para selección explícita (sin auto-normalización).
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
          varianteId: payload.varianteId,
        },
        {
          onSuccess: () => {
            setProductoSeleccionado(null);
            setVarianteSeleccionadaId(undefined);
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
              onCategoriaChange={(categoriaId) => {
                setCategoriaActiva(categoriaId);
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
              totalProductos={totalProductosSinExtras}
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

      {/* ── Modal: Selector de Variantes ── */}
      {varianteSelectorBase && variantesDelGrupo.length > 0 && (
        <VarianteSelectorModal
          productoBase={varianteSelectorBase}
          variantes={variantesDelGrupo}
          onSeleccionar={handleVarianteSeleccionada}
          onCerrar={() => {
            setVarianteSelectorBase(null);
            setVariantesDelGrupo([]);
          }}
        />
      )}

      {/* ── Modal: Configurar Producto (Observaciones + Extras) ── */}
      {productoSeleccionado && (
        <ConfigurarProductoModal
          producto={productoSeleccionado}
          onConfirmar={handleConfirmarProducto}
          onCerrar={() => {
            setProductoSeleccionado(null);
            setVarianteSeleccionadaId(undefined);
          }}
          enviando={agregarProducto.isPending}
          varianteId={varianteSeleccionadaId}
          categorias={categorias}
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
