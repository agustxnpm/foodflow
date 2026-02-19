import type { AxiosResponse } from 'axios';
import apiClient from '../../../lib/apiClient';
import type {
  ProductoResponse,
  ProductoRequest,
} from '../types';

export const productosApi = {
  listar: (color?: string | null): Promise<AxiosResponse<ProductoResponse[]>> =>
    apiClient.get('/productos', { params: { color } }),

  crear: (data: ProductoRequest): Promise<AxiosResponse<ProductoResponse>> =>
    apiClient.post('/productos', data),

  consultar: (id: string): Promise<AxiosResponse<ProductoResponse>> =>
    apiClient.get(`/productos/${id}`),

  editar: (id: string, data: Partial<ProductoRequest>): Promise<AxiosResponse<ProductoResponse>> =>
    apiClient.put(`/productos/${id}`, data),

  eliminar: (id: string): Promise<AxiosResponse<void>> =>
    apiClient.delete(`/productos/${id}`),

  ajustarStock: (id: string, data: { cantidad: number; tipo: string }): Promise<AxiosResponse<unknown>> =>
    apiClient.patch(`/productos/${id}/stock`, data),
};
