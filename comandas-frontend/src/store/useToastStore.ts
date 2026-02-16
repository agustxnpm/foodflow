import { create } from 'zustand';

let toastId = 0;

/**
 * Tipos de toast disponibles
 */
export type ToastType = 'success' | 'error' | 'warning' | 'info';

/**
 * Configuración de entrada para crear un toast
 */
export interface ToastInput {
  message: string;
  type?: ToastType;
  duration?: number;
}

/**
 * Toast con ID asignado por el store
 */
export interface Toast {
  id: number;
  message: string;
  type: ToastType;
  duration: number;
}

/**
 * Interfaz del estado del store
 */
interface ToastStore {
  toasts: Toast[];
  addToast: (toast: ToastInput) => number;
  removeToast: (id: number) => void;
  clearToasts: () => void;
}

/**
 * Store de Zustand para gestión de Toasts
 * Maneja notificaciones temporales en la aplicación
 */
const useToastStore = create<ToastStore>((set) => ({
  toasts: [],

  /**
   * Agrega un nuevo toast
   * @param toast - Configuración del toast
   * @returns ID del toast creado
   */
  addToast: (toast: ToastInput): number => {
    const id = toastId++;
    const duration = toast.duration ?? 3000;

    set((state) => ({
      toasts: [
        ...state.toasts,
        {
          id,
          message: toast.message,
          type: toast.type ?? 'info',
          duration,
        },
      ],
    }));

    // Auto-remover después de la duración especificada
    if (duration > 0) {
      setTimeout(() => {
        set((state) => ({
          toasts: state.toasts.filter((t) => t.id !== id),
        }));
      }, duration);
    }

    return id;
  },

  /**
   * Remueve un toast específico por ID
   */
  removeToast: (id: number): void => {
    set((state) => ({
      toasts: state.toasts.filter((t) => t.id !== id),
    }));
  },

  /**
   * Limpia todos los toasts
   */
  clearToasts: (): void => {
    set({ toasts: [] });
  },
}));

export default useToastStore;
