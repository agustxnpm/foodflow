import apiClient from '../../../lib/apiClient';

export const mesasApi = {
  listar: () => apiClient.get('/mesas'),
  crear: (numero) => apiClient.post('/mesas', { numero }),
  abrir: (mesaId) => apiClient.post(`/mesas/${mesaId}/abrir`),
  eliminar: (mesaId) => apiClient.delete(`/mesas/${mesaId}`),
  consultarPedido: (mesaId) => apiClient.get(`/mesas/${mesaId}/pedido-actual`),
  cerrar: (mesaId, pagos) => apiClient.post(`/mesas/${mesaId}/cierre`, { pagos }),
  obtenerTicket: (mesaId) => apiClient.get(`/mesas/${mesaId}/ticket`),
  obtenerComanda: (mesaId) => apiClient.get(`/mesas/${mesaId}/comanda`),
};
