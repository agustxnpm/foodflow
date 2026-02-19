import type { AxiosResponse } from 'axios';
import apiClient from '../../../lib/apiClient';
import type {
  AgregarProductoRequest,
  AgregarProductoResponse,
  AplicarDescuentoManualResponse,
  DescuentoManualRequest,
} from '../types';

export const pedidosApi = {
  agregarProducto: (
    pedidoId: string,
    data: AgregarProductoRequest,
  ): Promise<AxiosResponse<AgregarProductoResponse>> =>
    apiClient.post(`/pedidos/${pedidoId}/items`, data),

  aplicarDescuentoGlobal: (
    pedidoId: string,
    data: DescuentoManualRequest,
  ): Promise<AxiosResponse<AplicarDescuentoManualResponse>> =>
    apiClient.post(`/pedidos/${pedidoId}/descuento-manual`, data),

  aplicarDescuentoPorItem: (
    pedidoId: string,
    itemId: string,
    data: DescuentoManualRequest,
  ): Promise<AxiosResponse<AplicarDescuentoManualResponse>> =>
    apiClient.post(`/pedidos/${pedidoId}/items/${itemId}/descuento-manual`, data),

  modificarCantidad: (
    pedidoId: string,
    itemId: string,
    cantidad: number,
  ): Promise<AxiosResponse<void>> =>
    apiClient.patch(`/pedidos/${pedidoId}/items/${itemId}`, { cantidad }),

  eliminarItem: (pedidoId: string, itemId: string): Promise<AxiosResponse<void>> =>
    apiClient.delete(`/pedidos/${pedidoId}/items/${itemId}`),

  reabrir: (pedidoId: string): Promise<AxiosResponse<void>> =>
    apiClient.post(`/pedidos/${pedidoId}/reapertura`),
};
