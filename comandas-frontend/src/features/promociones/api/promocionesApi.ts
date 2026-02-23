import type { AxiosResponse } from 'axios';
import apiClient from '../../../lib/apiClient';
import type {
  CrearPromocionCommand,
  EditarPromocionCommand,
  AsociarScopeCommand,
  PromocionResponse,
  EstadoPromocion,
} from '../types';

export const promocionesApi = {
  listar: (estado?: EstadoPromocion | null): Promise<AxiosResponse<PromocionResponse[]>> =>
    apiClient.get('/promociones', { params: estado ? { estado } : {} }),

  crear: (data: CrearPromocionCommand): Promise<AxiosResponse<PromocionResponse>> =>
    apiClient.post('/promociones', data),

  obtener: (id: string): Promise<AxiosResponse<PromocionResponse>> =>
    apiClient.get(`/promociones/${id}`),

  editar: (id: string, data: EditarPromocionCommand): Promise<AxiosResponse<PromocionResponse>> =>
    apiClient.put(`/promociones/${id}`, data),

  toggleEstado: (id: string, estado: 'ACTIVA' | 'INACTIVA'): Promise<AxiosResponse<PromocionResponse>> =>
    apiClient.patch(`/promociones/${id}/estado`, { estado }),

  eliminar: (id: string): Promise<AxiosResponse<void>> =>
    apiClient.delete(`/promociones/${id}`),

  asociarProductos: (id: string, items: AsociarScopeCommand['items']): Promise<AxiosResponse<void>> =>
    apiClient.put(`/promociones/${id}/alcance`, { items }),
};
