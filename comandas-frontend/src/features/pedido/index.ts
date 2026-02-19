/**
 * Módulo Pedido - Feature Index
 * Exporta los componentes, hooks y API públicos del módulo.
 */

// Páginas
export { default as PantallaPedido } from './pages/PantallaPedido';

// Componentes
export { default as ListaCategorias } from './components/ListaCategorias';
export { default as GrillaProductos } from './components/GrillaProductos';
export { default as TicketPedido } from './components/TicketPedido';

// Hooks
export {
  useAgregarProducto,
  useAplicarDescuentoGlobal,
  useAplicarDescuentoPorItem,
  useModificarCantidad,
  useEliminarItem,
  useReabrirPedido,
} from './hooks/usePedido';

// API
export { pedidosApi } from './api/pedidosApi';

// Tipos
export type {
  ItemPedido,
  ItemDetalle,
  ItemDescuento,
  AgregarProductoRequest,
  AgregarProductoResponse,
  ModificarCantidadItemRequest,
  DescuentoManualRequest,
  DetallePedidoResponse,
  AplicarDescuentoManualResponse,
} from './types';
