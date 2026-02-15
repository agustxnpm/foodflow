import apiClient from '../../../lib/apiClient';

export const pedidosApi = {
  agregarProducto: (pedidoId, data) => apiClient.post(`/pedidos/${pedidoId}/items`, data),
  aplicarDescuentoGlobal: (pedidoId, data) => apiClient.post(`/pedidos/${pedidoId}/descuento-manual`, data),
  aplicarDescuentoPorItem: (pedidoId, itemId, data) => apiClient.post(`/pedidos/${pedidoId}/items/${itemId}/descuento-manual`, data),
  modificarCantidad: (pedidoId, itemId, cantidad) => apiClient.patch(`/pedidos/${pedidoId}/items/${itemId}`, { cantidad }),
  eliminarItem: (pedidoId, itemId) => apiClient.delete(`/pedidos/${pedidoId}/items/${itemId}`),
  reabrir: (pedidoId) => apiClient.post(`/pedidos/${pedidoId}/reapertura`),
};
