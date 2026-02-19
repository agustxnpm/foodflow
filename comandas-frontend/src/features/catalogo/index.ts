export { productosApi } from './api/productosApi';
export {
  useProductos,
  useProducto,
  useCrearProducto,
  useEditarProducto,
  useEliminarProducto,
  useAjustarStock,
} from './hooks/useProductos';
export type {
  ProductoRequest,
  ProductoResponse,
  TipoMovimientoStock,
} from './types';
