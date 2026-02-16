import useToastStore from '../store/useToastStore';

/**
 * Tipos de toast disponibles
 */
export type ToastType = 'success' | 'error' | 'warning' | 'info';

/**
 * Configuración para un toast personalizado
 */
export interface ToastConfig {
  message: string;
  type?: ToastType;
  duration?: number;
}

/**
 * Interfaz del hook useToast
 */
export interface UseToast {
  success: (message: string, duration?: number) => void;
  error: (message: string, duration?: number) => void;
  warning: (message: string, duration?: number) => void;
  info: (message: string, duration?: number) => void;
  custom: (config: ToastConfig) => void;
}

/**
 * Hook para mostrar notificaciones toast
 * Proporciona métodos convenientes para cada tipo de notificación
 * 
 * @example
 * const toast = useToast();
 * toast.success('Pedido guardado correctamente');
 * toast.error('Error al procesar el pago');
 */
const useToast = (): UseToast => {
  const addToast = useToastStore((state) => state.addToast);

  return {
    /**
     * Muestra un toast de éxito (verde)
     */
    success: (message: string, duration?: number) => {
      addToast({ message, type: 'success', duration });
    },

    /**
     * Muestra un toast de error (rojo)
     */
    error: (message: string, duration?: number) => {
      addToast({ message, type: 'error', duration });
    },

    /**
     * Muestra un toast de advertencia (amarillo)
     */
    warning: (message: string, duration?: number) => {
      addToast({ message, type: 'warning', duration });
    },

    /**
     * Muestra un toast informativo (azul)
     */
    info: (message: string, duration?: number) => {
      addToast({ message, type: 'info', duration });
    },

    /**
     * Muestra un toast personalizado
     */
    custom: (config: ToastConfig) => {
      addToast(config);
    },
  };
};

export default useToast;
