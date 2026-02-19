import apiClient from '../../../lib/apiClient';
import type { Mesa, CrearMesaRequest, CerrarMesaRequest } from '../types';
import type { DetallePedidoResponse } from '../../pedido/types';

/**
 * Cliente API para operaciones sobre mesas
 * Todas las respuestas son tipadas según el dominio
 */
export const mesasApi = {
  /**
   * HU-02: Listar todas las mesas del local
   */
  listar: async (): Promise<Mesa[]> => {
    const response = await apiClient.get<Mesa[]>('/mesas');
    return response.data;
  },

  /**
   * HU-15: Crear nueva mesa
   */
  crear: async (numero: number): Promise<Mesa> => {
    const dto: CrearMesaRequest = { numero };
    const response = await apiClient.post<Mesa>('/mesas', dto);
    return response.data;
  },

  /**
   * HU-03: Abrir mesa y crear pedido inicial
   */
  abrir: async (mesaId: string): Promise<void> => {
    await apiClient.post(`/mesas/${mesaId}/abrir`);
  },

  /**
   * HU-16: Eliminar mesa (solo si está LIBRE)
   */
  eliminar: async (mesaId: string): Promise<void> => {
    await apiClient.delete(`/mesas/${mesaId}`);
  },

  /**
   * HU-06: Consultar pedido actual de una mesa
   */
  consultarPedido: async (mesaId: string): Promise<DetallePedidoResponse> => {
    const response = await apiClient.get<DetallePedidoResponse>(`/mesas/${mesaId}/pedido-actual`);
    return response.data;
  },

  /**
   * HU-04, HU-12: Cerrar mesa y finalizar pedido
   */
  cerrar: async (mesaId: string, dto: CerrarMesaRequest): Promise<void> => {
    await apiClient.post(`/mesas/${mesaId}/cierre`, dto);
  },

  /**
   * Obtener ticket de la mesa (impresión)
   */
  obtenerTicket: async (mesaId: string): Promise<Blob> => {
    const response = await apiClient.get(`/mesas/${mesaId}/ticket`, {
      responseType: 'blob',
    });
    return response.data;
  },

  /**
   * Obtener comanda de cocina
   */
  obtenerComanda: async (mesaId: string): Promise<Blob> => {
    const response = await apiClient.get(`/mesas/${mesaId}/comanda`, {
      responseType: 'blob',
    });
    return response.data;
  },
};
