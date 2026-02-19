import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { mesasApi } from '../api/mesasApi';
import type { CerrarMesaRequest, CerrarMesaResponse, PagoRequest } from '../types';
import type { ComandaImpresionResponse, TicketImpresionResponse } from '../../pedido/types-impresion';

/**
 * HU-02: Hook para listar todas las mesas del local
 */
export function useMesas() {
  return useQuery({
    queryKey: ['mesas'],
    queryFn: () => mesasApi.listar(),
  });
}

/**
 * HU-06: Hook para consultar el pedido actual de una mesa
 */
export function usePedidoMesa(mesaId?: string) {
  return useQuery({
    queryKey: ['pedido', mesaId],
    queryFn: () => mesasApi.consultarPedido(mesaId!),
    enabled: !!mesaId,
  });
}

/**
 * HU-15: Hook para crear nueva mesa en el salón
 * Invalida el listado de mesas tras la creación
 */
export function useCrearMesa() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (numero: number) => mesasApi.crear(numero),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mesas'], exact: false });
    },
    onError: (error) => {
      console.error('[useCrearMesa] Error al crear mesa:', error);
    },
  });
}

/**
 * HU-03: Hook para abrir mesa y crear pedido inicial
 * Invalida mesas para refrescar estados (LIBRE → ABIERTA)
 */
export function useAbrirMesa() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (mesaId: string) => mesasApi.abrir(mesaId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mesas'], exact: false });
    },
    onError: (error) => {
      console.error('[useAbrirMesa] Error al abrir mesa:', error);
    },
  });
}

/**
 * HU-04 & HU-12: Hook para cerrar mesa y finalizar pedido
 * CRÍTICO: Invalida múltiples dominios (mesas, pedido, reporte-caja) para arqueo.
 * Devuelve CerrarMesaResponse con snapshot contable congelado.
 */
export function useCerrarMesa() {
  const queryClient = useQueryClient();
  
  return useMutation<CerrarMesaResponse, Error, { mesaId: string; pagos: PagoRequest[] }>({
    mutationFn: async ({ mesaId, pagos }) => {
      const dto: CerrarMesaRequest = { pagos };
      return mesasApi.cerrar(mesaId, dto);
    },
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
 * HU-16: Hook para eliminar mesa del salón
 * Solo permite eliminar mesas LIBRES sin pedidos activos
 */
export function useEliminarMesa() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (mesaId: string) => mesasApi.eliminar(mesaId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mesas'], exact: false });
    },
    onError: (error) => {
      console.error('[useEliminarMesa] Error al eliminar mesa:', error);
    },
  });
}

/**
 * HU-29: Obtener ticket de venta para preview/impresión.
 * Se invoca manualmente (mutation) para obtener datos estructurados.
 */
export function useObtenerTicket() {
  return useMutation<TicketImpresionResponse, Error, string>({
    mutationFn: (mesaId: string) => mesasApi.obtenerTicket(mesaId),
  });
}

/**
 * HU-05: Obtener comanda operativa para cocina/barra.
 * Se invoca manualmente (mutation) para enviar a cocina.
 */
export function useObtenerComanda() {
  return useMutation<ComandaImpresionResponse, Error, string>({
    mutationFn: (mesaId: string) => mesasApi.obtenerComanda(mesaId),
  });
}
