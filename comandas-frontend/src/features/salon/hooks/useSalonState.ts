import { useState, useCallback, useMemo } from 'react';
import type { Mesa } from '../types';
import { useMesas, useAbrirMesa } from './useMesas';
import useToast from '../../../hooks/useToast';

/**
 * Hook que encapsula el estado operativo del Salón.
 *
 * Responsabilidades:
 * - Proveer la lista de mesas desde el backend vía useMesas()
 * - Abrir el modal POS (`PantallaPedido`) al hacer click en una mesa
 * - Si la mesa está LIBRE, primero abre el pedido y luego muestra el modal
 * - Si la mesa está ABIERTA, muestra el modal directamente
 *
 * DECISIÓN: Se usa modal overlay en lugar de navegación a ruta separada.
 * Esto evita desorientar al operador — el salón siempre está "detrás"
 * y el modal se cierra exclusivamente con el botón "Atrás".
 */
export function useSalonState() {
  const toast = useToast();

  // ID de la mesa cuyo modal POS está abierto (null = cerrado)
  const [mesaSeleccionadaId, setMesaSeleccionadaId] = useState<string | null>(null);

  // Mesa LIBRE pendiente de confirmación (pre-modal)
  const [mesaPendienteApertura, setMesaPendienteApertura] = useState<Mesa | null>(null);

  const {
    data: mesas = [],
    isLoading: cargandoMesas,
    isError: errorMesas,
  } = useMesas();

  const abrirMesaMutation = useAbrirMesa();

  const mesasAbiertas = useMemo(
    () => mesas.filter((m) => m.estado === 'ABIERTA'),
    [mesas]
  );

  /**
   * Click en una mesa:
   * - ABIERTA → abre modal POS directamente
   * - LIBRE → muestra pre-modal de confirmación
   */
  const handleMesaClick = useCallback(
    (mesa: Mesa) => {
      if (mesa.estado === 'ABIERTA') {
        setMesaSeleccionadaId(mesa.id);
        return;
      }

      // Mesa LIBRE: mostrar pre-modal de confirmación
      setMesaPendienteApertura(mesa);
    },
    []
  );

  /** Confirmar apertura de mesa LIBRE desde el pre-modal */
  const confirmarAperturaMesa = useCallback(() => {
    if (!mesaPendienteApertura) return;

    const mesa = mesaPendienteApertura;
    abrirMesaMutation.mutate(mesa.id, {
      onSuccess: () => {
        toast.success(`Mesa ${mesa.numero} abierta`);
        setMesaPendienteApertura(null);
        setMesaSeleccionadaId(mesa.id);
      },
      onError: (error: any) => {
        const mensaje =
          error?.response?.data?.message || 'Error al abrir mesa';
        toast.error(mensaje);
      },
    });
  }, [mesaPendienteApertura, abrirMesaMutation, toast]);

  /** Cancelar la apertura de mesa desde el pre-modal */
  const cancelarAperturaMesa = useCallback(() => {
    setMesaPendienteApertura(null);
  }, []);

  /** Cierra el modal POS y vuelve al salón */
  const cerrarPedido = useCallback(() => {
    setMesaSeleccionadaId(null);
  }, []);

  return {
    mesas,
    mesasAbiertas,
    cargandoMesas,
    errorMesas,
    handleMesaClick,
    abriendoMesa: abrirMesaMutation.isPending,
    mesaSeleccionadaId,
    cerrarPedido,
    mesaPendienteApertura,
    confirmarAperturaMesa,
    cancelarAperturaMesa,
  } as const;
}
