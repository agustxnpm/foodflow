import useToastStore from '../store/useToastStore';
import type { Toast, ToastType } from '../store/useToastStore';

/**
 * Props del componente ToastItem
 */
interface ToastItemProps extends Toast {
  onClose: (id: number) => void;
}

/**
 * Componente individual de Toast
 * Representa una notificación temporal
 */
const ToastItem = ({ id, message, type, onClose }: ToastItemProps) => {
  // Estilos según tipo de toast
  const typeStyles: Record<ToastType, string> = {
    success: 'bg-green-600 border-green-700',
    error: 'bg-red-600 border-red-700',
    warning: 'bg-yellow-500 border-yellow-600 text-gray-900',
    info: 'bg-gray-700 border-gray-600',
  };

  // Iconos según tipo
  const icons: Record<ToastType, string> = {
    success: '✓',
    error: '✕',
    warning: '⚠',
    info: 'ℹ',
  };

  return (
    <div
      className={`
        ${typeStyles[type] || typeStyles.info}
        min-w-[280px] max-w-md
        px-4 py-3
        rounded-lg border-2
        shadow-lg
        flex items-center gap-3
        animate-slide-in-right
        text-white
      `}
    >
      {/* Icono */}
      <span className="text-xl font-bold flex-shrink-0">
        {icons[type]}
      </span>

      {/* Mensaje */}
      <p className="flex-1 text-sm font-medium leading-tight">
        {message}
      </p>

      {/* Botón cerrar */}
      <button
        onClick={() => onClose(id)}
        className="
          flex-shrink-0
          ml-2
          text-white hover:text-gray-300
          transition-colors
          text-xl
          font-bold
          leading-none
        "
        aria-label="Cerrar notificación"
      >
        ×
      </button>
    </div>
  );
};

/**
 * Contenedor de Toasts
 * Gestiona y muestra todas las notificaciones activas
 * Se posiciona fijo en la esquina superior derecha
 */
const ToastContainer = () => {
  const toasts = useToastStore((state) => state.toasts);
  const removeToast = useToastStore((state) => state.removeToast);

  if (toasts.length === 0) return null;

  return (
    <div
      className="
        fixed top-20 right-4 z-[100]
        flex flex-col gap-3
        pointer-events-none
      "
      aria-live="polite"
      aria-atomic="true"
    >
      <div className="flex flex-col gap-3 pointer-events-auto">
        {toasts.map((toast) => (
          <ToastItem
            key={toast.id}
            {...toast}
            onClose={removeToast}
          />
        ))}
      </div>
    </div>
  );
};

export default ToastContainer;
