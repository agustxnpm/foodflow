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

// ── Interceptor de REQUEST: X-Local-Id + logging de debug ────────────────────
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const localId = import.meta.env.VITE_LOCAL_ID;

    if (!localId) {
      throw new Error('VITE_LOCAL_ID no configurado. Verificar archivo .env');
    }

    config.headers['X-Local-Id'] = localId;

    console.info(
      `[API Request] ${config.method?.toUpperCase()} ${config.baseURL}${config.url}`,
      config.data ? JSON.stringify(config.data) : '(sin body)'
    );

    return config;
  },
  (error) => {
    console.error('[API Request Error]', error);
    return Promise.reject(error);
  }
);

// ── Interceptor de RESPONSE: logging extremo de errores ──────────────────────
apiClient.interceptors.response.use(
  (response) => {
    console.info(
      `[API Response] ${response.status} ${response.config.method?.toUpperCase()} ${response.config.url}`
    );
    return response;
  },
  (error) => {
    if (error.response) {
      console.error(
        `[API Error] Status: ${error.response.status}`,
        `\nURL: ${error.config?.method?.toUpperCase()} ${error.config?.url}`,
        `\nHeaders:`, JSON.stringify(error.response.headers),
        `\nBody:`, JSON.stringify(error.response.data)
      );
    } else if (error.request) {
      console.error(
        '[API Network Error] Sin respuesta del servidor.',
        `\nURL: ${error.config?.method?.toUpperCase()} ${error.config?.url}`,
        `\nMessage: ${error.message}`
      );
    } else {
      console.error('[API Setup Error]', error.message);
    }
    return Promise.reject(error);
  }
);

export default apiClient;
