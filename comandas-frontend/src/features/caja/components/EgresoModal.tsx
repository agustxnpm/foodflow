import { useState } from 'react';
import { X, Loader2, Printer } from 'lucide-react';
import type { EgresoRequest } from '../types';

interface EgresoModalProps {
  /** Cierra el modal */
  onClose: () => void;
  /** Dispara la mutación de registro */
  onConfirmar: (data: EgresoRequest) => void;
  /** Estado de la mutación */
  isPending: boolean;
}

/**
 * Modal de registro de egreso manual de caja.
 *
 * Inputs:
 * - Monto: input numérico grande, visual destacado
 * - Motivo: textarea para justificación
 *
 * El botón de confirmación refleja el estado `isPending` del hook
 * `useRegistrarEgreso`, mostrando "Registrando..." mientras procesa.
 *
 * Incluye nota de que se imprimirá un comprobante (stub ESC/POS → Tauri).
 */
export default function EgresoModal({ onClose, onConfirmar, isPending }: EgresoModalProps) {
  const [monto, setMonto] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [error, setError] = useState('');

  const montoNumerico = parseFloat(monto);
  const montoValido = !isNaN(montoNumerico) && montoNumerico > 0;
  const descripcionValida = descripcion.trim().length >= 3;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!montoValido) {
      setError('El monto debe ser mayor a $0');
      return;
    }
    if (!descripcionValida) {
      setError('Ingresá un motivo (mínimo 3 caracteres)');
      return;
    }

    setError('');
    onConfirmar({
      monto: montoNumerico,
      descripcion: descripcion.trim(),
    });
  };

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 z-[60] bg-black/70 backdrop-blur-sm animate-backdrop-in"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Modal */}
      <div className="fixed inset-0 z-[70] flex items-center justify-center p-4">
        <div
          className={[
            'bg-neutral-900 border-2 border-neutral-700 rounded-2xl',
            'shadow-2xl shadow-black/60',
            'w-full max-w-md',
            'animate-modal-in',
          ].join(' ')}
          role="dialog"
          aria-modal="true"
          aria-labelledby="egreso-modal-title"
        >
          {/* Header */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-neutral-800">
            <h2
              id="egreso-modal-title"
              className="text-lg font-bold text-gray-100"
            >
              Registrar Egreso
            </h2>
            <button
              type="button"
              onClick={onClose}
              disabled={isPending}
              className={[
                'w-10 h-10 rounded-xl flex items-center justify-center',
                'text-gray-400 hover:text-gray-100 hover:bg-neutral-800',
                'transition-colors active:scale-95',
              ].join(' ')}
              aria-label="Cerrar"
            >
              <X size={20} />
            </button>
          </div>

          {/* Body */}
          <form onSubmit={handleSubmit} className="px-6 py-5 space-y-5">
            {/* Monto */}
            <div className="space-y-2">
              <label
                htmlFor="egreso-monto"
                className="block text-sm font-medium text-gray-400 uppercase tracking-wide"
              >
                Monto
              </label>
              <div className="relative">
                <span className="absolute left-4 top-1/2 -translate-y-1/2 text-2xl font-bold text-gray-500">
                  $
                </span>
                <input
                  id="egreso-monto"
                  type="number"
                  inputMode="decimal"
                  step="0.01"
                  min="0.01"
                  value={monto}
                  onChange={(e) => setMonto(e.target.value)}
                  disabled={isPending}
                  placeholder="0.00"
                  autoFocus
                  className={[
                    'w-full h-16 pl-10 pr-4',
                    'bg-neutral-800 border-2 border-neutral-700 rounded-xl',
                    'text-3xl font-bold font-mono text-gray-100 text-right',
                    'placeholder:text-neutral-600',
                    'focus:border-amber-500 focus:outline-none',
                    'transition-colors',
                    'disabled:opacity-50',
                  ].join(' ')}
                />
              </div>
            </div>

            {/* Motivo */}
            <div className="space-y-2">
              <label
                htmlFor="egreso-motivo"
                className="block text-sm font-medium text-gray-400 uppercase tracking-wide"
              >
                Motivo / Justificación
              </label>
              <textarea
                id="egreso-motivo"
                value={descripcion}
                onChange={(e) => setDescripcion(e.target.value)}
                disabled={isPending}
                placeholder="Ej: Productos de limpieza, cambio para delivery..."
                rows={3}
                className={[
                  'w-full px-4 py-3',
                  'bg-neutral-800 border-2 border-neutral-700 rounded-xl',
                  'text-base text-gray-200 resize-none',
                  'placeholder:text-neutral-600',
                  'focus:border-amber-500 focus:outline-none',
                  'transition-colors',
                  'disabled:opacity-50',
                ].join(' ')}
              />
            </div>

            {/* Error */}
            {error && (
              <p className="text-sm text-red-400 font-medium" role="alert">
                {error}
              </p>
            )}

            {/* Nota de impresión */}
            <div className="flex items-center gap-2 px-3 py-2 rounded-lg bg-neutral-800/60 border border-neutral-700">
              <Printer size={16} className="text-gray-500 shrink-0" />
              <p className="text-xs text-gray-500">
                Se imprimirá un comprobante de egreso automáticamente.
              </p>
            </div>

            {/* Botón confirmar */}
            <button
              type="submit"
              disabled={isPending || !montoValido || !descripcionValida}
              className={[
                'w-full h-14 rounded-xl font-semibold text-base',
                'flex items-center justify-center gap-3',
                'transition-all duration-150',
                'focus:outline-none focus-visible:ring-2 focus-visible:ring-amber-400',
                isPending
                  ? 'bg-neutral-700 text-gray-400 cursor-wait'
                  : montoValido && descripcionValida
                    ? 'bg-amber-600 hover:bg-amber-500 text-white active:scale-95'
                    : 'bg-neutral-700 text-gray-500 cursor-not-allowed',
              ].join(' ')}
            >
              {isPending ? (
                <>
                  <Loader2 size={20} className="animate-spin" />
                  Registrando...
                </>
              ) : (
                'Confirmar Egreso'
              )}
            </button>
          </form>
        </div>
      </div>
    </>
  );
}
