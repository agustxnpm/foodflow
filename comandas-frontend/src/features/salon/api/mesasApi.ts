import apiClient from '../../../lib/apiClient';
import type { Mesa, CrearMesaRequest, CerrarMesaRequest, CerrarMesaResponse } from '../types';
import type { DetallePedidoResponse } from '../../pedido/types';
import type { ComandaImpresionResponse, TicketImpresionResponse } from '../../pedido/types-impresion';

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
   * Devuelve snapshot contable congelado con pagos registrados.
   */
  cerrar: async (mesaId: string, dto: CerrarMesaRequest): Promise<CerrarMesaResponse> => {
    const response = await apiClient.post<CerrarMesaResponse>(`/mesas/${mesaId}/cierre`, dto);
    return response.data;
  },

  /**
   * HU-29: Obtener ticket de venta para el cliente
   * Devuelve estructura tipada para renderizado en preview/impresión.
   */
  obtenerTicket: async (mesaId: string): Promise<TicketImpresionResponse> => {
    const response = await apiClient.get<TicketImpresionResponse>(`/mesas/${mesaId}/ticket`);
    return response.data;
  },

  /**
   * HU-05: Obtener comanda operativa para cocina/barra
   * Devuelve estructura tipada sin información financiera.
   */
  obtenerComanda: async (mesaId: string): Promise<ComandaImpresionResponse> => {
    const response = await apiClient.get<ComandaImpresionResponse>(`/mesas/${mesaId}/comanda`);
    return response.data;
  },
};
