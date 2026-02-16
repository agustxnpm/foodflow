import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { promocionesApi } from '../api/promocionesApi';

export function usePromociones(estado = null) {
  return useQuery({
    queryKey: ['promociones', estado],
    queryFn: () => promocionesApi.listar(estado),
  });
}

export function usePromocion(id) {
  return useQuery({
    queryKey: ['promocion', id],
    queryFn: () => promocionesApi.obtener(id),
    enabled: !!id,
  });
}

/**
 * Crear nueva promoción.
 * Invalida el prefijo para actualizar listados y filtros.
 */
export function useCrearPromocion() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: promocionesApi.crear,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['promociones'], exact: false });
    },
    onError: (error) => {
      console.error('[useCrearPromocion] Error al crear promoción:', error);
    },
  });
}

/**
 * Editar promoción existente.
 * Invalida prefijo completo para actualizar detalles y listados.
 */
export function useEditarPromocion() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ id, ...data }) => promocionesApi.editar(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['promociones'], exact: false });
      queryClient.invalidateQueries({ queryKey: ['promocion'], exact: false });
    },
    onError: (error) => {
      console.error('[useEditarPromocion] Error al editar promoción:', error);
    },
  });
}

/**
 * Eliminar promoción.
 * Invalida todos los listados de promociones.
 */
export function useEliminarPromocion() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: promocionesApi.eliminar,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['promociones'], exact: false });
    },
    onError: (error) => {
      console.error('[useEliminarPromocion] Error al eliminar promoción:', error);
    },
  });
}

/**
 * Asociar productos a una promoción.
 * Invalida el prefijo para refrescar el alcance de la promoción.
 */
export function useAsociarProductos() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ id, items }) => promocionesApi.asociarProductos(id, items),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['promocion'], exact: false });
    },
    onError: (error) => {
      console.error('[useAsociarProductos] Error al asociar productos:', error);
    },
  });
}
