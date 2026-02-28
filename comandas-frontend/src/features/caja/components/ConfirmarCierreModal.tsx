import { AlertTriangle, X } from 'lucide-react';

interface ConfirmarCierreModalProps {
  onConfirmar: () => void;
  onCancelar: () => void;
  isPending: boolean;
}

/**
 * Modal de confirmación antes de cerrar la jornada de caja.
 *
 * Acción destructiva → requiere confirmación explícita.
 * Una vez cerrada, la jornada no puede reabrirse.
 *
 * Sigue el patrón de modales del design system:
 * - Backdrop clickeable para cancelar
 * - Paleta rojo/negro
 * - Botón primario destructivo (rojo)
 * - Botón secundario neutro (cancelar)
 */
export default function ConfirmarCierreModal({
  onConfirmar,
  onCancelar,
  isPending,
}: ConfirmarCierreModalProps) {
  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 z-50 bg-black/60 animate-backdrop-in"
        onClick={onCancelar}
      />

      {/* Modal */}
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 pointer-events-none">
        <div
          className="bg-neutral-900 border border-gray-800 rounded-xl w-full max-w-sm pointer-events-auto animate-modal-in"
          onClick={(e) => e.stopPropagation()}
        >
          {/* Header */}
          <div className="flex items-center justify-between px-5 py-4 border-b border-gray-800">
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-lg bg-red-600/20 flex items-center justify-center">
                <AlertTriangle size={18} className="text-red-400" />
              </div>
              <h2 className="text-base font-semibold text-gray-100">
                Cerrar jornada
              </h2>
            </div>
            <button
              onClick={onCancelar}
              disabled={isPending}
              className="p-2 rounded-lg hover:bg-gray-800 text-gray-400 transition-colors"
            >
              <X size={18} />
            </button>
          </div>

          {/* Body */}
          <div className="px-5 py-5">
            <p className="text-sm text-gray-300 leading-relaxed">
              Se registrará el cierre de la jornada con los totales actuales.
              <span className="block mt-2 text-xs text-gray-500">
                Esta acción no se puede deshacer.
              </span>
            </p>
          </div>

          {/* Footer */}
          <div className="flex gap-2.5 px-5 py-4 border-t border-gray-800">
            <button
              type="button"
              onClick={onCancelar}
              disabled={isPending}
              className={[
                'flex-1 h-11 rounded-xl font-medium text-sm',
                'bg-neutral-800 border border-neutral-700/80 text-gray-300',
                'hover:bg-neutral-700/80 hover:text-gray-100',
                'transition-all duration-150 active:scale-[0.97]',
                'focus:outline-none focus-visible:ring-2 focus-visible:ring-neutral-500',
              ].join(' ')}
            >
              Cancelar
            </button>

            <button
              type="button"
              onClick={onConfirmar}
              disabled={isPending}
              className={[
                'flex-1 h-11 rounded-xl font-medium text-sm',
                'flex items-center justify-center gap-2',
                'transition-all duration-150',
                'focus:outline-none focus-visible:ring-2 focus-visible:ring-red-500/50',
                isPending
                  ? 'bg-neutral-800 text-gray-500 cursor-wait'
                  : 'bg-red-600/90 hover:bg-red-500 text-white active:scale-[0.97]',
              ].join(' ')}
            >
              {isPending ? 'Cerrando…' : 'Sí, cerrar jornada'}
            </button>
          </div>
        </div>
      </div>
    </>
  );
}
