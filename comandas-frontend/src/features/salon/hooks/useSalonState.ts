import { useState, useCallback, useMemo } from 'react';
import type { Mesa } from '../types';
import { useMesas, usePedidoMesa, useAbrirMesa } from './useMesas';
import useToast from '../../../hooks/useToast';

/**
 * Hook que encapsula el estado operativo del Salón.
 *
 * Responsabilidades:
 * - Proveer la lista de mesas desde el backend vía useMesas() (React Query)
 * - Gestionar la selección de mesa y apertura del modal de detalle
 * - Consultar el pedido activo vía usePedidoMesa() (solo si mesa ABIERTA)
 * - Abrir mesas libres vía useAbrirMesa() desde el modal
 *
 * Comportamiento:
 * - Mesa LIBRE → modal con opción de abrir pedido
 * - Mesa ABIERTA → modal con detalle del pedido (skeleton loading real)
 *
 * DECISIÓN: Se separa la lógica del JSX para mantener SalonPage como orquestador puro.
 */
export function useSalonState() {
  const [mesaSeleccionada, setMesaSeleccionada] = useState<Mesa | null>(null);
  const [modalAbierto, setModalAbierto] = useState(false);
  const toast = useToast();

  // ── Data del backend ──
  const {
    data: mesas = [],
    isLoading: cargandoMesas,
    isError: errorMesas,
  } = useMesas();

  // Solo consulta el pedido si la mesa seleccionada está ABIERTA
  const consultarPedido =
    modalAbierto && mesaSeleccionada?.estado === 'ABIERTA'
      ? mesaSeleccionada.id
      : undefined;

  const {
    data: pedidoDetalle = null,
    isLoading: cargandoDetalle,
  } = usePedidoMesa(consultarPedido);

  const abrirMesaMutation = useAbrirMesa();

  const mesasAbiertas = useMemo(
    () => mesas.filter((m) => m.estado === 'ABIERTA'),
    [mesas]
  );

  // Abre el modal para cualquier mesa (libre u ocupada)
  const abrirDetalleMesa = useCallback((mesa: Mesa) => {
    setMesaSeleccionada(mesa);
    setModalAbierto(true);
  }, []);

  // Acción: abrir una mesa libre → crea pedido en el backend
  const handleAbrirMesa = useCallback(
    (mesaId: string) => {
      abrirMesaMutation.mutate(mesaId, {
        onSuccess: () => {
          const mesa = mesas.find((m) => m.id === mesaId);
          toast.success(`Mesa ${mesa?.numero ?? mesaId} abierta`);
          cerrarModal();
        },
        onError: (error: any) => {
          const mensaje =
            error?.response?.data?.message || 'Error al abrir mesa';
          toast.error(mensaje);
        },
      });
    },
    [abrirMesaMutation, mesas, toast]
  );

  const cerrarModal = useCallback(() => {
    setModalAbierto(false);
    setMesaSeleccionada(null);
  }, []);

  return {
    mesas,
    mesasAbiertas,
    cargandoMesas,
    errorMesas,
    mesaSeleccionada,
    modalAbierto,
    cargandoDetalle,
    pedidoDetalle,
    abrirDetalleMesa,
    handleAbrirMesa,
    abriendoMesa: abrirMesaMutation.isPending,
    cerrarModal,
  } as const;
}
