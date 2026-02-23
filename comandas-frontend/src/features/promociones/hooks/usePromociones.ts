import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { promocionesApi } from '../api/promocionesApi';
import type {
  CrearPromocionCommand,
  EditarPromocionCommand,
  EstadoPromocion,
  ItemScopeParams,
} from '../types';

export function usePromociones(estado: EstadoPromocion | null = null) {
  return useQuery({
    queryKey: ['promociones', estado],
    queryFn: () => promocionesApi.listar(estado),
    refetchInterval: 60_000,
  });
}

export function usePromocion(id: string | null) {
  return useQuery({
    queryKey: ['promocion', id],
    queryFn: () => promocionesApi.obtener(id!),
    enabled: !!id,
    refetchInterval: 60_000,
  });
}

/**
 * Crear nueva promoción.
 * Invalida el prefijo para actualizar listados y filtros.
 */
export function useCrearPromocion() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CrearPromocionCommand) => promocionesApi.crear(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['promociones'], exact: false });
    },
    onError: (error: unknown) => {
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
    mutationFn: ({ id, ...data }: EditarPromocionCommand & { id: string }) =>
      promocionesApi.editar(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['promociones'], exact: false });
      queryClient.invalidateQueries({ queryKey: ['promocion'], exact: false });
    },
    onError: (error: unknown) => {
      console.error('[useEditarPromocion] Error al editar promoción:', error);
    },
  });
}

/**
 * Eliminar promoción.
 * Invalida todos los listados de promociones.
 */
export function useToggleEstadoPromocion() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, estado }: { id: string; estado: 'ACTIVA' | 'INACTIVA' }) =>
      promocionesApi.toggleEstado(id, estado),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['promociones'], exact: false });
      queryClient.invalidateQueries({ queryKey: ['promocion'], exact: false });
    },
    onError: (error: unknown) => {
      console.error('[useToggleEstadoPromocion] Error al cambiar estado:', error);
    },
  });
}

export function useEliminarPromocion() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => promocionesApi.eliminar(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['promociones'], exact: false });
    },
    onError: (error: unknown) => {
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
    mutationFn: ({ id, items }: { id: string; items: ItemScopeParams[] }) =>
      promocionesApi.asociarProductos(id, items),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['promocion'], exact: false });
    },
    onError: (error: unknown) => {
      console.error('[useAsociarProductos] Error al asociar productos:', error);
    },
  });
}
