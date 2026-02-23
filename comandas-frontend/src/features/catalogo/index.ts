export { productosApi } from './api/productosApi';
export {
  useProductos,
  useProducto,
  useCrearProducto,
  useEditarProducto,
  useEliminarProducto,
  useAjustarStock,
} from './hooks/useProductos';
export { default as VistaCatalogo } from './components/VistaCatalogo';
export { default as ProductoModal } from './components/ProductoModal';
export { default as AjusteStockModal } from './components/AjusteStockModal';
export type {
  ProductoRequest,
  ProductoResponse,
  PromocionActivaInfo,
  TipoMovimientoStock,
} from './types';
