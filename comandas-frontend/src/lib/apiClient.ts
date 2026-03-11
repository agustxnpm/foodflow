import axios from 'axios';
import type { AxiosInstance, InternalAxiosRequestConfig } from 'axios';

// ── Base URL ─────────────────────────────────────────────────────────────────
// En DEV, usamos '/api' relativo → Vite proxy lo reenvía a http://127.0.0.1:8080
// En PROD (Tauri NSIS), NO hay proxy. El frontend se sirve desde el protocolo
// custom de Tauri (http://tauri.localhost) y las requests relativas NO llegan
// al backend. Necesitamos la URL absoluta al backend embebido.
const resolvedBaseURL = import.meta.env.PROD
  ? (import.meta.env.VITE_API_URL || 'http://localhost:8080/api')
  : '/api';

// Cliente HTTP base para FoodFlow
const apiClient: AxiosInstance = axios.create({
  baseURL: resolvedBaseURL,
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

// ── Interceptor de RESPONSE: logging + diagnóstico en producción ─────────────
apiClient.interceptors.response.use(
  (response) => {
    // En producción, loggear el payload completo para diagnosticar diferencias
    // entre el JSON real del backend offline (SQLite) y lo esperado por la UI.
    // En Tauri, estos logs van al archivo en disco vía tauri-plugin-log.
    if (import.meta.env.PROD) {
      console.log(
        `[PROD-DEBUG] ${response.config.method?.toUpperCase()} ${response.config.url} Status: ${response.status} Payload:`,
        JSON.stringify(response.data),
      );
    } else {
      console.info(
        `[API Response] ${response.status} ${response.config.method?.toUpperCase()} ${response.config.url}`
      );
    }
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
