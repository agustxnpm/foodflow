import { Plus, Trash2, X, Loader2 } from 'lucide-react';
import { useCrearMesa } from '../hooks/useMesas';
import useToast from '../../../hooks/useToast';
import type { Mesa } from '../types';

interface SalonControlsProps {
  mesas: Mesa[];
  modoEdicion: boolean;
  onToggleModoEdicion: () => void;
}

/**
 * Controles del salón
 * HU-15: Crear nueva mesa (auto-incrementa número)
 * HU-16: Activar modo edición para eliminar mesas
 */
export default function SalonControls({
  mesas,
  modoEdicion,
  onToggleModoEdicion,
}: SalonControlsProps) {
  const crearMesa = useCrearMesa();
  const toast = useToast();

  /** Calcula el próximo número: max(existentes) + 1, o 1 si no hay mesas */
  const calcularProximoNumero = (): number => {
    if (!mesas || mesas.length === 0) return 1;
    const maxNumero = Math.max(...mesas.map((m) => m.numero));
    return maxNumero + 1;
  };

  const handleCrearMesa = () => {
    const numero = calcularProximoNumero();

    crearMesa.mutate(numero, {
      onSuccess: () => {
        toast.success(`Mesa ${numero} creada`);
      },
      onError: (error: any) => {
        const mensaje = error?.response?.data?.message || 'Error al crear mesa';
        toast.error(mensaje);
      },
    });
  };

  return (
    <div className="flex flex-wrap items-center gap-2">
      {/* ── Botón: Nueva Mesa (auto-incremento) ── */}
      <button
        onClick={handleCrearMesa}
        disabled={crearMesa.isPending}
        className="
          group flex items-center gap-2
          px-4 h-10
          bg-red-600 text-white
          rounded-lg text-sm font-semibold
          hover:bg-red-500
          active:scale-95
          disabled:opacity-60 disabled:cursor-not-allowed
          transition-all duration-150
          shadow-sm shadow-red-950/40
        "
      >
        {crearMesa.isPending ? (
          <Loader2 size={16} className="animate-spin" />
        ) : (
          <Plus size={16} strokeWidth={2.5} className="group-hover:rotate-90 transition-transform duration-200" />
        )}
        <span>{crearMesa.isPending ? 'Creando…' : 'Nueva Mesa'}</span>
      </button>

      {/* ── Botón: Modo Eliminación ── */}
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
            <Trash2 size={14} strokeWidth={2} />
            <span>Eliminar</span>
          </>
        )}
      </button>
    </div>
  );
}
