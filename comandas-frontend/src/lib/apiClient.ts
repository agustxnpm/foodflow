import axios from 'axios';
import type { AxiosInstance, InternalAxiosRequestConfig } from 'axios';

// Cliente HTTP base para FoodFlow
const apiClient: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor obligatorio: X-Local-Id (multi-tenant por fila)
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // En modo desktop/offline, el localId se configura en .env
    // Debe coincidir con app.context.local-id del backend
    const localId = import.meta.env.VITE_LOCAL_ID;
    
    if (!localId) {
      throw new Error('VITE_LOCAL_ID no configurado. Verificar archivo .env');
    }
    
    config.headers['X-Local-Id'] = localId;
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Interceptor de respuesta para manejo de errores
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      // Error del servidor
      console.error('API Error:', error.response.status, error.response.data);
    } else if (error.request) {
      // Error de red
      console.error('Network Error:', error.message);
    }
    return Promise.reject(error);
  }
);

export default apiClient;
