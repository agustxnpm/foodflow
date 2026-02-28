import { AlertTriangle, X, ArrowRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface AlertaMesasAbiertasProps {
  /** Mensaje del error (viene de MesasAbiertasError.message) */
  mensaje: string;
  /** Cantidad de mesas abiertas (puede ser undefined si el backend no lo informa) */
  mesasAbiertas?: number;
  /** Cierra la alerta */
  onDismiss: () => void;
}

/**
 * Alerta crítica bloqueante: mesas abiertas impiden cierre de jornada.
 *
 * Se renderiza como un banner/modal superpuesto (z-[60]) con:
 * - Fondo rojo oscuro semi-transparente (bg-red-950/95)
 * - Borde rojo
 * - Ícono de advertencia
 * - Mensaje descriptivo
 * - Botón para ir al Salón a cerrar las mesas
 * - Botón para descartar la alerta
 *
 * El diseño busca que el operador no pueda ignorar la alerta:
 * es visualmente agresiva y cubre parte de la pantalla.
 */
export default function AlertaMesasAbiertas({
  mensaje,
  mesasAbiertas,
  onDismiss,
}: AlertaMesasAbiertasProps) {
  const navigate = useNavigate();

  const handleIrAlSalon = () => {
    onDismiss();
    navigate('/');
  };

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 z-[60] bg-black/70 backdrop-blur-sm animate-backdrop-in"
        onClick={onDismiss}
        aria-hidden="true"
      />

      {/* Panel de alerta */}
      <div className="fixed inset-0 z-[70] flex items-center justify-center p-4">
        <div
          className={[
            'w-full max-w-lg',
            'bg-red-950/95 border-2 border-red-700 rounded-2xl',
            'shadow-2xl shadow-red-950/50',
            'animate-modal-in',
            'overflow-hidden',
          ].join(' ')}
          role="alertdialog"
          aria-modal="true"
          aria-labelledby="alerta-mesas-titulo"
          aria-describedby="alerta-mesas-desc"
        >
          {/* Barra superior roja */}
          <div className="h-1.5 bg-red-600" />

          <div className="p-6 space-y-5">
            {/* Header */}
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-4">
                <div className="w-14 h-14 rounded-full bg-red-900/60 flex items-center justify-center shrink-0">
                  <AlertTriangle size={28} className="text-red-400" />
                </div>
                <div>
                  <h3
                    id="alerta-mesas-titulo"
                    className="text-xl font-bold text-white"
                  >
                    No se puede cerrar la jornada
                  </h3>
                  {mesasAbiertas != null && (
                    <p className="text-sm text-red-300 mt-0.5">
                      {mesasAbiertas} {mesasAbiertas === 1 ? 'mesa abierta' : 'mesas abiertas'}
                    </p>
                  )}
                </div>
              </div>
              <button
                type="button"
                onClick={onDismiss}
                className={[
                  'w-10 h-10 rounded-xl flex items-center justify-center shrink-0',
                  'text-red-400 hover:text-white hover:bg-red-800/60',
                  'transition-colors',
                ].join(' ')}
                aria-label="Cerrar alerta"
              >
                <X size={20} />
              </button>
            </div>

            {/* Mensaje */}
            <p
              id="alerta-mesas-desc"
              className="text-base text-red-200 leading-relaxed"
            >
              {mensaje}
            </p>

            {/* Separador */}
            <div className="border-t border-red-800/50" />

            {/* Instrucciones */}
            <div className="bg-red-900/40 rounded-xl px-4 py-3">
              <p className="text-sm text-red-200">
                <strong className="text-white">¿Qué hacer?</strong>{' '}
                Dirigite al Salón y cerrá todas las mesas abiertas antes de cerrar la jornada.
              </p>
            </div>

            {/* Botones */}
            <div className="flex gap-3">
              <button
                type="button"
                onClick={onDismiss}
                className={[
                  'flex-1 h-14 rounded-xl font-medium text-base',
                  'bg-red-900/60 border border-red-800 text-red-200',
                  'hover:bg-red-900 transition-colors active:scale-95',
                ].join(' ')}
              >
                Entendido
              </button>
              <button
                type="button"
                onClick={handleIrAlSalon}
                className={[
                  'flex-1 h-14 rounded-xl font-semibold text-base',
                  'flex items-center justify-center gap-2',
                  'bg-red-600 hover:bg-red-500 text-white',
                  'transition-all duration-150 active:scale-95',
                ].join(' ')}
              >
                Ir al Salón
                <ArrowRight size={18} />
              </button>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
