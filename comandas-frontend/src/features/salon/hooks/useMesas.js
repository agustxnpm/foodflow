import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { mesasApi } from '../api/mesasApi';

export function useMesas() {
  return useQuery({
    queryKey: ['mesas'],
    queryFn: () => mesasApi.listar(),
  });
}

export function usePedidoMesa(mesaId) {
  return useQuery({
    queryKey: ['pedido', mesaId],
    queryFn: () => mesasApi.consultarPedido(mesaId),
    enabled: !!mesaId,
  });
}

/**
 * HU-03: Crear nueva mesa en el salón.
 * Invalida el listado de mesas.
 */
export function useCrearMesa() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: mesasApi.crear,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mesas'], exact: false });
    },
    onError: (error) => {
      console.error('[useCrearMesa] Error al crear mesa:', error);
    },
  });
}

/**
 * HU-02: Abrir mesa y crear pedido inicial.
 * Invalida mesas para refrescar estados (LIBRE → ABIERTA).
 */
export function useAbrirMesa() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: mesasApi.abrir,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mesas'], exact: false });
    },
    onError: (error) => {
      console.error('[useAbrirMesa] Error al abrir mesa:', error);
    },
  });
}

/**
 * HU-04 & HU-12: Cerrar mesa y finalizar pedido.
 * CRÍTICO: Invalida múltiples dominios (mesas, pedido, reporte-caja) para arqueo.
 */
export function useCerrarMesa() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ mesaId, pagos }) => mesasApi.cerrar(mesaId, pagos),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mesas'], exact: false });
      queryClient.invalidateQueries({ queryKey: ['pedido'], exact: false });
      queryClient.invalidateQueries({ queryKey: ['reporte-caja'], exact: false });
    },
    onError: (error) => {
      console.error('[useCerrarMesa] Error al cerrar mesa:', error);
    },
  });
}

/**
 * HU-03: Eliminar mesa del salón.
 * Solo permite eliminar mesas LIBRES sin pedidos activos.
 */
export function useEliminarMesa() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: mesasApi.eliminar,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mesas'], exact: false });
    },
    onError: (error) => {
      console.error('[useEliminarMesa] Error al eliminar mesa:', error);
    },
  });
}

export function useObtenerTicket(mesaId) {
  return useQuery({
    queryKey: ['ticket', mesaId],
    queryFn: () => mesasApi.obtenerTicket(mesaId),
    enabled: false, // Se ejecuta manualmente
  });
}

export function useObtenerComanda(mesaId) {
  return useQuery({
    queryKey: ['comanda', mesaId],
    queryFn: () => mesasApi.obtenerComanda(mesaId),
    enabled: false, // Se ejecuta manualmente
  });
}
