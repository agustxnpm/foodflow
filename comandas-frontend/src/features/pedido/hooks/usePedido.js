import { useMutation, useQueryClient } from '@tanstack/react-query';
import { pedidosApi } from '../api/pedidosApi';

/**
 * HU-05: Agregar producto al pedido (Aggregate Root).
 * Invalida el prefijo ['pedido'] para refrescar totales recalculados por el backend.
 */
export function useAgregarProducto() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ pedidoId, ...data }) => pedidosApi.agregarProducto(pedidoId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pedido'], exact: false });
    },
    onError: (error) => {
      console.error('[useAgregarProducto] Error al agregar producto:', error);
    },
  });
}

/**
 * HU-14: Aplicar descuento global al pedido.
 * Invalida el prefijo para actualizar todos los detalles y totales.
 */
export function useAplicarDescuentoGlobal() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ pedidoId, ...data }) => pedidosApi.aplicarDescuentoGlobal(pedidoId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pedido'], exact: false });
    },
    onError: (error) => {
      console.error('[useAplicarDescuentoGlobal] Error al aplicar descuento:', error);
    },
  });
}

/**
 * HU-14: Aplicar descuento por ítem específico.
 * Invalida el prefijo del aggregate root.
 */
export function useAplicarDescuentoPorItem() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ pedidoId, itemId, ...data }) => pedidosApi.aplicarDescuentoPorItem(pedidoId, itemId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pedido'], exact: false });
    },
    onError: (error) => {
      console.error('[useAplicarDescuentoPorItem] Error al aplicar descuento:', error);
    },
  });
}

/**
 * HU-20: Modificar cantidad de un ítem del pedido.
 * Recalcula totales en el backend y refresca la UI.
 */
export function useModificarCantidad() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ pedidoId, itemId, cantidad }) => pedidosApi.modificarCantidad(pedidoId, itemId, cantidad),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pedido'], exact: false });
    },
    onError: (error) => {
      console.error('[useModificarCantidad] Error al modificar cantidad:', error);
    },
  });
}

/**
 * HU-21: Eliminar ítem del pedido.
 * Invalida el aggregate root para recalcular totales.
 */
export function useEliminarItem() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ pedidoId, itemId }) => pedidosApi.eliminarItem(pedidoId, itemId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pedido'], exact: false });
    },
    onError: (error) => {
      console.error('[useEliminarItem] Error al eliminar ítem:', error);
    },
  });
}

/**
 * HU-14: Reabrir pedido cerrado (corrección de errores operativos).
 * Invalida pedido y mesas para actualizar el estado de ambos dominios.
 */
export function useReabrirPedido() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: pedidosApi.reabrir,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pedido'], exact: false });
      queryClient.invalidateQueries({ queryKey: ['mesas'], exact: false });
    },
    onError: (error) => {
      console.error('[useReabrirPedido] Error al reabrir pedido:', error);
    },
  });
}
