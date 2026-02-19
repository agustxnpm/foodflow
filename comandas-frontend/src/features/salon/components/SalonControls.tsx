import { useState } from 'react';
import { Plus, Edit3, X } from 'lucide-react';
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
    <div className="flex flex-wrap items-center gap-3">
      {/* Botón: Nueva Mesa */}
      {!mostrarInput && (
        <button
          onClick={() => setMostrarInput(true)}
          className="
            flex items-center gap-2
            px-4 h-12
            bg-red-600
            text-white
            rounded-lg
            font-semibold
            hover:bg-red-700
            transition-colors
            active:scale-95
          "
        >
          <Plus size={20} />
          <span>Nueva Mesa</span>
        </button>
      )}

      {/* Input para crear mesa */}
      {mostrarInput && (
        <div className="flex items-center gap-2">
          <input
            type="number"
            value={numeroMesa}
            onChange={(e) => setNumeroMesa(e.target.value)}
            placeholder="Número de mesa"
            className="
              w-32
              h-12
              px-4
              bg-neutral-800
              border-2
              border-gray-600
              rounded-lg
              text-gray-100
              focus:border-red-600
              focus:outline-none
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
            disabled={crearMesa.isPending}
            className="
              px-4 h-12
              bg-green-600
              text-white
              rounded-lg
              font-semibold
              hover:bg-green-700
              disabled:bg-gray-600
              disabled:cursor-not-allowed
              transition-colors
            "
          >
            {crearMesa.isPending ? 'Creando...' : 'Crear'}
          </button>

          <button
            onClick={handleCancelar}
            disabled={crearMesa.isPending}
            className="
              px-4 h-12
              bg-gray-700
              text-gray-300
              rounded-lg
              font-semibold
              hover:bg-gray-600
              transition-colors
            "
          >
            Cancelar
          </button>
        </div>
      )}

      {/* Botón: Modo Edición */}
      <button
        onClick={onToggleModoEdicion}
        className={`
          flex items-center gap-2
          px-4 h-12
          rounded-lg
          font-semibold
          transition-colors
          active:scale-95
          
          ${modoEdicion 
            ? 'bg-red-600 text-white hover:bg-red-700' 
            : 'bg-neutral-800 text-gray-300 border-2 border-gray-600 hover:border-gray-500'
          }
        `}
      >
        {modoEdicion ? (
          <>
            <X size={20} />
            <span>Salir de Edición</span>
          </>
        ) : (
          <>
            <Edit3 size={20} />
            <span>Modo Edición</span>
          </>
        )}
      </button>
    </div>
  );
}
