import type { AxiosResponse } from 'axios';
import apiClient from '../../../lib/apiClient';
import type {
  ProductoResponse,
  ProductoRequest,
  VarianteRequest,
  VarianteResponse,
} from '../types';

export const productosApi = {
  listar: (categoriaId?: string | null, activo?: boolean): Promise<AxiosResponse<ProductoResponse[]>> =>
    apiClient.get('/productos', { params: { categoriaId, activo } }),

  crear: (data: ProductoRequest): Promise<AxiosResponse<ProductoResponse>> =>
    apiClient.post('/productos', data),

  consultar: (id: string): Promise<AxiosResponse<ProductoResponse>> =>
    apiClient.get(`/productos/${id}`),

  editar: (id: string, data: Partial<ProductoRequest>): Promise<AxiosResponse<ProductoResponse>> =>
    apiClient.put(`/productos/${id}`, data),

  eliminar: (id: string): Promise<AxiosResponse<void>> =>
    apiClient.delete(`/productos/${id}`),

  ajustarStock: (id: string, data: { cantidad: number; tipo: string; motivo: string }): Promise<AxiosResponse<unknown>> =>
    apiClient.patch(`/productos/${id}/stock`, data),

  /**
   * Lista todas las variantes de un producto (hermanas del mismo grupo).
   * GET /api/productos/{productoId}/variantes
   * Retorna lista vacía si el producto no tiene variantes.
   */
  listarVariantes: (productoId: string): Promise<AxiosResponse<ProductoResponse[]>> =>
    apiClient.get(`/productos/${productoId}/variantes`),

  /**
   * Crea una variante de un producto base existente.
   * POST /api/productos/{productoBaseId}/variantes
   * Si el base no tiene grupo todavía, se crea automáticamente.
   */
  crearVariante: (productoBaseId: string, data: VarianteRequest): Promise<AxiosResponse<VarianteResponse>> =>
    apiClient.post(`/productos/${productoBaseId}/variantes`, data),
};
