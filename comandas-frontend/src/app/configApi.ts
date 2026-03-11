import apiClient from '../lib/apiClient';

export const configApi = {
  /** Lista impresoras detectadas en el SO del backend */
  listarImpresoras: () =>
    apiClient.get<string[]>('/config/impresoras'),

  /** Obtiene la impresora predeterminada guardada */
  obtenerImpresoraPredeterminada: () =>
    apiClient.get<{ impresora: string }>('/config/impresora-predeterminada'),

  /** Guarda la impresora seleccionada como predeterminada */
  guardarImpresoraPredeterminada: (nombre: string) =>
    apiClient.post<{ message: string }>('/config/impresora-predeterminada', { nombre }),
};
