import apiClient from '../../../lib/apiClient';

export const cajaApi = {
  registrarEgreso: (data) => apiClient.post('/caja/egresos', data),
  obtenerReporte: (fecha) => apiClient.get('/caja/reporte', { params: { fecha } }),
};
