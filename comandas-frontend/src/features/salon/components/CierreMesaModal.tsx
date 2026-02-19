import { useState } from 'react';
import { X, DollarSign, CreditCard, QrCode } from 'lucide-react';
import type { MedioPago, PagoRequest } from '../types';
import type { DetallePedidoResponse } from '../../pedido/types';
import { useCerrarMesa } from '../hooks/useMesas';
import useToast from '../../../hooks/useToast';

interface CierreMesaModalProps {
  mesaId: string;
  pedido: DetallePedidoResponse;
  onClose: () => void;
  onSuccess: () => void;
}

/**
 * Modal de cierre de mesa con liquidación final
 * HU-04: Cerrar mesa
 * HU-12: Cierre de Mesa y Liquidación Final
 */
export default function CierreMesaModal({ 
  mesaId,
  pedido, 
  onClose, 
  onSuccess 
}: CierreMesaModalProps) {
  const [medioPagoSeleccionado, setMedioPagoSeleccionado] = useState<MedioPago | null>(null);
  const cerrarMesa = useCerrarMesa();
  const toast = useToast();

  const mediosPago: Array<{ tipo: MedioPago; icono: typeof DollarSign; label: string }> = [
    { tipo: 'EFECTIVO', icono: DollarSign, label: 'Efectivo' },
    { tipo: 'TARJETA', icono: CreditCard, label: 'Tarjeta' },
    { tipo: 'QR', icono: QrCode, label: 'QR' },
  ];

  const handleCerrar = () => {
    if (!medioPagoSeleccionado) {
      toast.warning('Selecciona un medio de pago');
      return;
    }

    if (pedido.totalParcial <= 0) {
      toast.error('El total debe ser mayor a 0');
      return;
    }

    const pago: PagoRequest = {
      medio: medioPagoSeleccionado,
      monto: pedido.totalParcial,
    };

    cerrarMesa.mutate(
      { mesaId, pagos: [pago] },
      {
        onSuccess: () => {
          toast.success(`Mesa ${pedido.numeroMesa} cerrada exitosamente`);
          onSuccess();
          onClose();
        },
        onError: (error: any) => {
          const mensaje = error?.response?.data?.message || 'Error al cerrar mesa';
          toast.error(mensaje);
        },
      }
    );
  };

  return (
    <div className="
      fixed inset-0 z-50
      flex items-center justify-center
      bg-black/70
      p-4
    ">
      <div className="
        bg-neutral-900
        border-2
        border-gray-700
        rounded-lg
        max-w-md
        w-full
        p-6
        space-y-6
      ">
        {/* Header */}
        <div className="flex items-center justify-between">
          <h2 className="text-2xl font-bold text-gray-100">
            Cerrar Mesa {pedido.numeroMesa}
          </h2>
          <button
            onClick={onClose}
            disabled={cerrarMesa.isPending}
            className="
              text-gray-400
              hover:text-gray-200
              transition-colors
            "
          >
            <X size={24} />
          </button>
        </div>

        {/* Detalle del pedido */}
        <div className="space-y-3 py-4 border-y border-gray-700">
          {/* Subtotal */}
          <div className="flex justify-between text-gray-300">
            <span>Subtotal:</span>
            <span className="font-mono">${pedido.subtotal.toFixed(2)}</span>
          </div>

          {/* Descuentos */}
          {pedido.totalDescuentos > 0 && (
            <div className="flex justify-between text-green-500">
              <span>Descuentos:</span>
              <span className="font-mono">-${pedido.totalDescuentos.toFixed(2)}</span>
            </div>
          )}

          {/* Total */}
          <div className="flex justify-between text-2xl font-bold text-gray-100 pt-2 border-t border-gray-700">
            <span>Total:</span>
            <span className="font-mono text-red-500">
              ${pedido.totalParcial.toFixed(2)}
            </span>
          </div>
        </div>

        {/* Selección de Medio de Pago */}
        <div className="space-y-3">
          <label className="block text-sm font-semibold text-gray-300">
            Medio de Pago
          </label>
          
          <div className="grid grid-cols-3 gap-3">
            {mediosPago.map(({ tipo, icono: Icon, label }) => (
              <button
                key={tipo}
                onClick={() => setMedioPagoSeleccionado(tipo)}
                disabled={cerrarMesa.isPending}
                className={`
                  flex flex-col items-center justify-center
                  h-24
                  rounded-lg
                  border-2
                  transition-all
                  active:scale-95
                  
                  ${medioPagoSeleccionado === tipo
                    ? 'bg-red-600 border-red-500 text-white'
                    : 'bg-neutral-800 border-gray-600 text-gray-400 hover:border-gray-500'
                  }
                  
                  ${cerrarMesa.isPending && 'opacity-50 cursor-not-allowed'}
                `}
              >
                <Icon size={32} />
                <span className="text-xs font-semibold mt-2">{label}</span>
              </button>
            ))}
          </div>
        </div>

        {/* Botón Confirmar */}
        <button
          onClick={handleCerrar}
          disabled={cerrarMesa.isPending || !medioPagoSeleccionado}
          className="
            w-full
            h-14
            bg-red-600
            text-white
            rounded-lg
            font-bold
            text-lg
            hover:bg-red-700
            disabled:bg-gray-600
            disabled:cursor-not-allowed
            transition-colors
          "
        >
          {cerrarMesa.isPending ? 'Cerrando...' : 'Confirmar y Cerrar Mesa'}
        </button>
      </div>
    </div>
  );
}
