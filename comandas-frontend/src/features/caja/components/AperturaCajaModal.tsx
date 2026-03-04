import { useState, useRef, useEffect } from 'react';
import { Vault, AlertTriangle, X, ArrowDownLeft } from 'lucide-react';

interface AperturaCajaModalProps {
  /** Saldo remanente de la última jornada cerrada. Null si no hay historial. */
  saldoSugerido: number | null;
  /** Callback al confirmar la apertura con el monto declarado */
  onConfirmar: (montoInicial: number) => void;
  /** Callback para cerrar el modal */
  onCancelar: () => void;
  /** Indica si la mutación de apertura está en curso */
  isPending: boolean;
}

/**
 * Modal de apertura de caja — flujo bloqueante.
 *
 * Permite al operador declarar el fondo inicial de efectivo
 * antes de iniciar la jornada. La acción es irreversible dentro
 * del mismo día operativo.
 *
 * Características:
 * - Input numérico grande con autofocus (touch-friendly)
 * - Botón de relleno rápido con saldo del cierre anterior (si disponible)
 * - Banner de advertencia (amber) sobre la responsabilidad del monto declarado
 * - Respeta paleta del design system: neutral-900 / neutral-800 / emerald-600
 *
 * Sigue el patrón de ConfirmarCierreModal (backdrop, animate-modal-in, etc.).
 */
export default function AperturaCajaModal({
  saldoSugerido,
  onConfirmar,
  onCancelar,
  isPending,
}: AperturaCajaModalProps) {
  const [monto, setMonto] = useState<string>(
    saldoSugerido != null && saldoSugerido > 0 ? saldoSugerido.toString() : '',
  );
  const inputRef = useRef<HTMLInputElement>(null);

  // Autofocus al montar
  useEffect(() => {
    // Timeout para esperar la animación del modal
    const timer = setTimeout(() => inputRef.current?.focus(), 100);
    return () => clearTimeout(timer);
  }, []);

  const montoNumerico = parseFloat(monto) || 0;
  const esValido = montoNumerico >= 0 && monto.trim().length > 0;

  const handleUsarSaldo = () => {
    if (saldoSugerido != null && saldoSugerido > 0) {
      setMonto(saldoSugerido.toString());
      inputRef.current?.focus();
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (esValido && !isPending) {
      onConfirmar(montoNumerico);
    }
  };

  return (
    <>
      {/* Backdrop */}
      <div className="fixed inset-0 z-50 bg-black/60 animate-backdrop-in" />

      {/* Modal */}
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 pointer-events-none">
        <div
          className="bg-neutral-900 border border-neutral-800 rounded-xl w-full max-w-md pointer-events-auto animate-modal-in"
          onClick={(e) => e.stopPropagation()}
        >
          {/* Header */}
          <div className="flex items-center justify-between px-5 py-4 border-b border-neutral-800">
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-lg bg-emerald-600/20 flex items-center justify-center">
                <Vault size={18} className="text-emerald-400" />
              </div>
              <h2 className="text-base font-semibold text-gray-100">
                Abrir jornada de caja
              </h2>
            </div>
            <button
              onClick={onCancelar}
              disabled={isPending}
              className="p-2 rounded-lg hover:bg-neutral-800 text-gray-400 transition-colors"
            >
              <X size={18} />
            </button>
          </div>

          {/* Body */}
          <form onSubmit={handleSubmit} className="px-5 py-5 space-y-4">
            {/* Descripción */}
            <p className="text-sm text-gray-400 leading-relaxed">
              Declarás el efectivo con el que arranca la caja hoy.
              Este valor se usa para calcular el arqueo al cierre.
            </p>

            {/* Input de monto */}
            <div className="space-y-2">
              <label
                htmlFor="fondo-inicial"
                className="block text-xs text-gray-500 uppercase tracking-wider font-medium"
              >
                Fondo inicial
              </label>
              <div className="relative">
                <span className="absolute left-4 top-1/2 -translate-y-1/2 text-2xl text-gray-500 font-mono">
                  $
                </span>
                <input
                  ref={inputRef}
                  id="fondo-inicial"
                  type="number"
                  inputMode="decimal"
                  min="0"
                  step="0.01"
                  placeholder="0.00"
                  value={monto}
                  onChange={(e) => setMonto(e.target.value)}
                  disabled={isPending}
                  className={[
                    'w-full h-16 pl-10 pr-4 rounded-xl',
                    'bg-neutral-800 border border-neutral-700/80',
                    'text-2xl font-mono text-gray-100 placeholder-gray-600',
                    'focus:outline-none focus:ring-2 focus:ring-emerald-500/50 focus:border-emerald-600/50',
                    'transition-all duration-150',
                    'disabled:opacity-50 disabled:cursor-wait',
                    '[appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none',
                  ].join(' ')}
                />
              </div>
            </div>

            {/* Botón de saldo sugerido */}
            {saldoSugerido != null && saldoSugerido > 0 && (
              <button
                type="button"
                onClick={handleUsarSaldo}
                disabled={isPending}
                className={[
                  'w-full flex items-center justify-between gap-2 px-4 py-3 rounded-xl',
                  'bg-neutral-800/80 border border-neutral-700/50',
                  'text-sm text-gray-300 hover:bg-neutral-700/60 hover:text-gray-100',
                  'transition-all duration-150 active:scale-[0.98]',
                  'disabled:opacity-50 disabled:cursor-wait',
                ].join(' ')}
              >
                <span className="flex items-center gap-2">
                  <ArrowDownLeft size={14} className="text-emerald-400" />
                  Usar saldo del cierre anterior
                </span>
                <span className="font-mono text-emerald-400">
                  ${saldoSugerido.toLocaleString('es-AR', { minimumFractionDigits: 2 })}
                </span>
              </button>
            )}

            {/* Banner de advertencia */}
            <div className="flex gap-3 p-3 rounded-lg bg-amber-950/30 border border-amber-800/30">
              <AlertTriangle
                size={16}
                className="text-amber-500 shrink-0 mt-0.5"
              />
              <p className="text-xs text-amber-400/90 leading-relaxed">
                El monto declarado como fondo inicial es responsabilidad del operador.
                Una vez abierta la jornada, este valor no puede modificarse.
              </p>
            </div>

            {/* Acciones */}
            <div className="flex gap-2.5 pt-1">
              <button
                type="button"
                onClick={onCancelar}
                disabled={isPending}
                className={[
                  'flex-1 h-12 rounded-xl font-medium text-sm',
                  'bg-neutral-800 border border-neutral-700/80 text-gray-300',
                  'hover:bg-neutral-700/80 hover:text-gray-100',
                  'transition-all duration-150 active:scale-[0.97]',
                  'focus:outline-none focus-visible:ring-2 focus-visible:ring-neutral-500',
                ].join(' ')}
              >
                Cancelar
              </button>

              <button
                type="submit"
                disabled={!esValido || isPending}
                className={[
                  'flex-1 h-12 rounded-xl font-semibold text-sm',
                  'flex items-center justify-center gap-2',
                  'transition-all duration-150',
                  'focus:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500/50',
                  !esValido || isPending
                    ? 'bg-neutral-800 text-gray-500 cursor-not-allowed'
                    : 'bg-emerald-600 hover:bg-emerald-500 text-white active:scale-[0.97]',
                ].join(' ')}
              >
                {isPending ? 'Abriendo…' : 'Abrir jornada'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </>
  );
}
