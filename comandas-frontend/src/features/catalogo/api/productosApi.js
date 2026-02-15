import apiClient from '../../../lib/apiClient';

export const productosApi = {
  listar: (color) => apiClient.get('/productos', { params: { color } }),
  crear: (data) => apiClient.post('/productos', data),
  consultar: (id) => apiClient.get(`/productos/${id}`),
  editar: (id, data) => apiClient.put(`/productos/${id}`, data),
  eliminar: (id) => apiClient.delete(`/productos/${id}`),
  ajustarStock: (id, data) => apiClient.patch(`/productos/${id}/stock`, data),
};
