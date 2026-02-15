import apiClient from '../../../lib/apiClient';

export const promocionesApi = {
  listar: (estado) => apiClient.get('/promociones', { params: { estado } }),
  crear: (data) => apiClient.post('/promociones', data),
  obtener: (id) => apiClient.get(`/promociones/${id}`),
  editar: (id, data) => apiClient.put(`/promociones/${id}`, data),
  eliminar: (id) => apiClient.delete(`/promociones/${id}`),
  asociarProductos: (id, items) => apiClient.put(`/promociones/${id}/alcance`, { items }),
};
