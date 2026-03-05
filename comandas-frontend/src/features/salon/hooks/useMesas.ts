import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { mesasApi } from '../api/mesasApi';
import type { CerrarMesaRequest, CerrarMesaResponse, PagoRequest } from '../types';
import type { ComandaImpresionResponse, TicketImpresionResponse, EnviarComandaResponse, TicketVentaEscPosResponse } from '../../pedido/types-impresion';

/**
 * HU-02: Hook para listar todas las mesas del local.
 * Polling cada 15s para mantener estados de mesa actualizados
 * en Tauri, donde no hay refetchOnWindowFocus.
 */
export function useMesas() {
  return useQuery({
    queryKey: ['mesas'],
    queryFn: () => mesasApi.listar(),
    refetchInterval: 15_000,
  });
}

/**
 * HU-06: Hook para consultar el pedido actual de una mesa.
 * Polling cada 10s para mantener totales y estados del pedido
 * actualizados mientras el modal POS está abierto.
 */
export function usePedidoMesa(mesaId?: string) {
  return useQuery({
    queryKey: ['pedido', mesaId],
    queryFn: () => mesasApi.consultarPedido(mesaId!),
    enabled: !!mesaId,
    refetchInterval: 10_000,
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
      queryClient.invalidateQueries({ queryKey: ['reporte-ventas-productos'], exact: false });
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

/**
 * HU-29: Enviar comanda a cocina con generación de buffer ESC/POS.
 *
 * Muta estado en el backend: actualiza ultimoEnvioCocina del pedido.
 * Retorna el buffer ESC/POS en Base64 para enviarlo a la impresora vía printerService.
 *
 * Invalida la query del pedido para refrescar los badges NUEVO/ENVIADO
 * que ahora provienen del campo esNuevo del backend.
 */
export function useEnviarComandaCocina() {
  const queryClient = useQueryClient();

  return useMutation<EnviarComandaResponse, Error, string>({
    mutationFn: (mesaId: string) => mesasApi.enviarCocina(mesaId),
    onSuccess: (_data, mesaId) => {
      // Refrescar los datos del pedido para actualizar badges esNuevo
      queryClient.invalidateQueries({ queryKey: ['pedido', mesaId] });
    },
  });
}

/**
 * HU-29: Reimprimir comanda completa (todos los ítems, sin actualizar timestamp).
 * No invalida queries porque no muta estado.
 */
export function useReimprimirComanda() {
  return useMutation<EnviarComandaResponse, Error, string>({
    mutationFn: (mesaId: string) => mesasApi.reimprimirComanda(mesaId),
  });
}

/**
 * HU-29: Generar ticket de venta ESC/POS para impresión térmica.
 * Solo lectura — no muta estado. Retorna Base64 para enviar a impresora vía printerService.
 */
export function useGenerarTicketEscPos() {
  return useMutation<TicketVentaEscPosResponse, Error, string>({
    mutationFn: (mesaId: string) => mesasApi.generarTicketEscPos(mesaId),
  });
}
