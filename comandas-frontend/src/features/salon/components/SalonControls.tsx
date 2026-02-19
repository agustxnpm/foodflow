import { useState } from 'react';
import { Plus, Pencil, X, Check, Loader2 } from 'lucide-react';
import { useCrearMesa } from '../hooks/useMesas';
import useToast from '../../../hooks/useToast';

interface SalonControlsProps {
  modoEdicion: boolean;
  onToggleModoEdicion: () => void;
}

/**
 * Controles del salón
 * HU-15: Crear nueva mesa
 * HU-16: Activar modo edición para eliminar mesas
 */
export default function SalonControls({ 
  modoEdicion, 
  onToggleModoEdicion 
}: SalonControlsProps) {
  const [mostrarInput, setMostrarInput] = useState(false);
  const [numeroMesa, setNumeroMesa] = useState('');
  const crearMesa = useCrearMesa();
  const toast = useToast();

  const handleCrear = () => {
    const numero = parseInt(numeroMesa);

    if (!numero || numero <= 0) {
      toast.error('Ingresa un número válido (mayor a 0)');
      return;
    }

    crearMesa.mutate(numero, {
      onSuccess: () => {
        toast.success(`Mesa ${numero} creada exitosamente`);
        setNumeroMesa('');
        setMostrarInput(false);
      },
      onError: (error: any) => {
        const mensaje = error?.response?.data?.message || 'Error al crear mesa';
        toast.error(mensaje);
      },
    });
  };

  const handleCancelar = () => {
    setNumeroMesa('');
    setMostrarInput(false);
  };

  return (
    <div className="flex flex-wrap items-center gap-2">
      {/* ── Botón / Input: Nueva Mesa ── */}
      {!mostrarInput ? (
        <button
          onClick={() => setMostrarInput(true)}
          className="
            group flex items-center gap-2
            px-4 h-10
            bg-red-600 text-white
            rounded-lg text-sm font-semibold
            hover:bg-red-500
            active:scale-95
            transition-all duration-150
            shadow-sm shadow-red-950/40
          "
        >
          <Plus size={16} strokeWidth={2.5} className="group-hover:rotate-90 transition-transform duration-200" />
          <span>Nueva Mesa</span>
        </button>
      ) : (
        <div className="flex items-center gap-1.5 bg-neutral-900 border border-neutral-700 rounded-lg p-1 pl-3">
          <span className="text-xs text-gray-500 font-medium whitespace-nowrap">Mesa Nº</span>
          <input
            type="number"
            value={numeroMesa}
            onChange={(e) => setNumeroMesa(e.target.value)}
            placeholder="—"
            className="
              w-16 h-8
              px-2
              bg-transparent
              text-gray-100 text-sm font-bold tabular-nums
              placeholder:text-gray-700
              focus:outline-none
              [&::-webkit-inner-spin-button]:appearance-none
              [&::-webkit-outer-spin-button]:appearance-none
            "
            autoFocus
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleCrear();
              if (e.key === 'Escape') handleCancelar();
            }}
            disabled={crearMesa.isPending}
          />

          <button
            onClick={handleCrear}
            disabled={crearMesa.isPending || !numeroMesa.trim()}
            className="
              w-8 h-8 rounded-md
              flex items-center justify-center
              bg-red-600 text-white
              hover:bg-red-500
              disabled:bg-neutral-700 disabled:text-neutral-500 disabled:cursor-not-allowed
              transition-colors
            "
            aria-label="Confirmar nueva mesa"
          >
            {crearMesa.isPending ? (
              <Loader2 size={14} className="animate-spin" />
            ) : (
              <Check size={14} strokeWidth={3} />
            )}
          </button>

          <button
            onClick={handleCancelar}
            disabled={crearMesa.isPending}
            className="
              w-8 h-8 rounded-md
              flex items-center justify-center
              text-gray-500
              hover:bg-neutral-800 hover:text-gray-300
              transition-colors
            "
            aria-label="Cancelar"
          >
            <X size={14} strokeWidth={2.5} />
          </button>
        </div>
      )}

      {/* ── Botón: Modo Edición ── */}
      <button
        onClick={onToggleModoEdicion}
        className={[
          'group flex items-center gap-2',
          'px-4 h-10 rounded-lg text-sm font-semibold',
          'active:scale-95 transition-all duration-150',
          modoEdicion
            ? 'bg-red-600/15 text-red-400 border border-red-600/40 hover:bg-red-600/25'
            : 'bg-neutral-800/80 text-gray-400 border border-neutral-700 hover:border-neutral-600 hover:text-gray-300',
        ].join(' ')}
      >
        {modoEdicion ? (
          <>
            <X size={16} strokeWidth={2.5} />
            <span>Salir</span>
          </>
        ) : (
          <>
            <Pencil size={14} strokeWidth={2} />
            <span>Editar</span>
          </>
        )}
      </button>
    </div>
  );
}
