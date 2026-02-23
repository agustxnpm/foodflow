import { useState } from 'react';
import { X, Check, ArrowDownCircle, ArrowUpCircle } from 'lucide-react';
import { useAjustarStock } from '../hooks/useProductos';
import type { ProductoResponse, TipoMovimientoStock } from '../types';

interface TipoMovimientoConfig {
  valor: TipoMovimientoStock;
  label: string;
  icon: typeof ArrowUpCircle;
  color: string;
  /** Signo de la cantidad: +1 (ingreso) o -1 (egreso) */
  signo: 1 | -1;
}

const TIPOS_MOVIMIENTO: TipoMovimientoConfig[] = [
  {
    valor: 'INGRESO_MERCADERIA',
    label: 'Ingreso',
    icon: ArrowUpCircle,
    color: 'text-green-400 border-green-500/50 bg-green-500/10',
    signo: 1,
  },
  {
    valor: 'AJUSTE_MANUAL',
    label: 'Egreso / Ajuste',
    icon: ArrowDownCircle,
    color: 'text-yellow-400 border-yellow-500/50 bg-yellow-500/10',
    signo: -1,
  },
];

interface AjusteStockModalProps {
  producto: ProductoResponse;
  onClose: () => void;
}

/**
 * Modal para ajustar el stock de un producto.
 *
 * Muestra el stock actual y solicita:
 * - Cantidad a ajustar (siempre positiva, el signo lo define el tipo)
 * - Tipo de movimiento (Ingreso / Egreso-Ajuste)
 * - Motivo obligatorio (auditoría)
 *
 * Decisión: Se simplificaron los tipos de movimiento para el UI.
 * VENTA y REAPERTURA_PEDIDO son automáticos del sistema, el operador
 * solo puede hacer INGRESO_MERCADERIA o AJUSTE_MANUAL.
 */
export default function AjusteStockModal({ producto, onClose }: AjusteStockModalProps) {
  const ajustarStock = useAjustarStock();

  const [cantidad, setCantidad] = useState('');
  const [tipoSeleccionado, setTipoSeleccionado] = useState<TipoMovimientoConfig>(TIPOS_MOVIMIENTO[0]);
  const [motivo, setMotivo] = useState('');
  const [error, setError] = useState<string | null>(null);

  const stockActual = producto.stockActual ?? 0;

  const handleAjustarStock = () => {
    setError(null);

    const cantidadNum = parseInt(cantidad, 10);
    if (isNaN(cantidadNum) || cantidadNum <= 0) {
      setError('La cantidad debe ser un número positivo');
      return;
    }

    if (!motivo.trim()) {
      setError('El motivo es obligatorio para auditoría');
      return;
    }

    // Aplicar signo según tipo de movimiento
    const cantidadFinal = cantidadNum * tipoSeleccionado.signo;

    ajustarStock.mutate(
      {
        id: producto.id,
        cantidad: cantidadFinal,
        tipo: tipoSeleccionado.valor,
        motivo: motivo.trim(),
      },
      { onSuccess: onClose }
    );
  };

  // Preview del stock resultante
  const cantidadNum = parseInt(cantidad, 10) || 0;
  const stockResultante = stockActual + cantidadNum * tipoSeleccionado.signo;

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 z-50 bg-black/60 animate-backdrop-in"
        onClick={onClose}
      />

      {/* Modal */}
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 pointer-events-none">
        <div
          className="bg-neutral-900 border border-gray-800 rounded-xl w-full max-w-md pointer-events-auto animate-modal-in"
          onClick={(e) => e.stopPropagation()}
        >
          {/* Header */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-gray-800">
            <div>
              <h2 className="text-lg font-semibold text-text-primary">Ajustar Stock</h2>
              <p className="text-sm text-text-secondary">{producto.nombre}</p>
            </div>
            <button
              onClick={onClose}
              className="p-2 rounded-lg hover:bg-gray-800 text-gray-400 transition-colors"
            >
              <X size={18} />
            </button>
          </div>

          {/* Body */}
          <div className="px-6 py-5 space-y-5">
            {/* Stock actual → resultante */}
            <div className="flex items-center justify-center gap-4 p-4 bg-background-card rounded-lg">
              <div className="text-center">
                <p className="text-xs text-text-secondary">Actual</p>
                <p className="text-2xl font-mono font-bold text-text-primary">{stockActual}</p>
              </div>
              <span className="text-gray-500 text-xl">→</span>
              <div className="text-center">
                <p className="text-xs text-text-secondary">Resultante</p>
                <p
                  className={`text-2xl font-mono font-bold ${
                    stockResultante < 0 ? 'text-red-400' : 'text-green-400'
                  }`}
                >
                  {cantidadNum > 0 ? stockResultante : '—'}
                </p>
              </div>
            </div>

            {/* Tipo de movimiento (selector visual) */}
            <div className="space-y-2">
              <label className="text-sm text-text-secondary">Tipo de movimiento</label>
              <div className="grid grid-cols-2 gap-2">
                {TIPOS_MOVIMIENTO.map((tipo) => {
                  const Icon = tipo.icon;
                  const isActive = tipoSeleccionado.valor === tipo.valor;
                  return (
                    <button
                      key={tipo.valor}
                      type="button"
                      onClick={() => setTipoSeleccionado(tipo)}
                      className={[
                        'flex items-center gap-2 p-3 rounded-lg border transition-all text-sm font-medium',
                        isActive
                          ? tipo.color + ' border-current'
                          : 'text-gray-400 border-gray-700 hover:border-gray-500',
                      ].join(' ')}
                    >
                      <Icon size={18} />
                      <span>{tipo.label}</span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Cantidad */}
            <div className="flex flex-col gap-1">
              <label className="text-sm text-text-secondary">Cantidad</label>
              <input
                type="number"
                value={cantidad}
                onChange={(e) => setCantidad(e.target.value)}
                placeholder="0"
                min="1"
                className="min-h-[48px] px-4 bg-background-card border border-gray-700 rounded-lg text-text-primary font-mono text-lg focus:border-primary focus:outline-none"
                autoFocus
              />
            </div>

            {/* Motivo */}
            <div className="flex flex-col gap-1">
              <label className="text-sm text-text-secondary">
                Motivo <span className="text-red-400">*</span>
              </label>
              <input
                type="text"
                value={motivo}
                onChange={(e) => setMotivo(e.target.value)}
                placeholder="Ej: Compra a proveedor, rotura, error de conteo..."
                className="min-h-[48px] px-4 bg-background-card border border-gray-700 rounded-lg text-text-primary focus:border-primary focus:outline-none"
              />
            </div>

            {/* Error */}
            {error && (
              <p className="text-sm text-red-500">{error}</p>
            )}
          </div>

          {/* Footer */}
          <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-gray-800">
            <button
              onClick={onClose}
              className="btn-secondary text-sm !min-h-[42px] px-5"
              disabled={ajustarStock.isPending}
            >
              Cancelar
            </button>
            <button
              onClick={handleAjustarStock}
              className="btn-primary text-sm !min-h-[42px] px-6 flex items-center gap-2"
              disabled={ajustarStock.isPending}
            >
              {ajustarStock.isPending ? (
                <span className="animate-pulse">Ajustando...</span>
              ) : (
                <>
                  <Check size={16} />
                  <span>Confirmar Ajuste</span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </>
  );
}
