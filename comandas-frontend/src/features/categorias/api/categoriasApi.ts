import type { AxiosResponse } from 'axios';
import apiClient from '../../../lib/apiClient';
import type { CategoriaResponse, CategoriaRequest } from '../types';

/**
 * API client para operaciones CRUD de categorías.
 * Consume los endpoints REST de CategoriaController del backend.
 */
export const categoriasApi = {
  listar: (): Promise<AxiosResponse<CategoriaResponse[]>> =>
    apiClient.get('/categorias'),

  crear: (data: CategoriaRequest): Promise<AxiosResponse<CategoriaResponse>> =>
    apiClient.post('/categorias', data),

  editar: (id: string, data: CategoriaRequest): Promise<AxiosResponse<CategoriaResponse>> =>
    apiClient.put(`/categorias/${id}`, data),

  eliminar: (id: string): Promise<AxiosResponse<void>> =>
    apiClient.delete(`/categorias/${id}`),
};
