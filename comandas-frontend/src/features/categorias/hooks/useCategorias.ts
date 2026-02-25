import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { categoriasApi } from '../api/categoriasApi';
import type { CategoriaResponse, CategoriaRequest } from '../types';

/**
 * Lista todas las categorías del local, ordenadas por `orden`.
 *
 * queryKey: ['categorias'] para invalidación granular.
 * Polling cada 60s: las categorías rara vez cambian durante el servicio.
 * staleTime de 30s para evitar refetches innecesarios en navegación.
 */
export function useCategorias() {
  return useQuery<CategoriaResponse[]>({
    queryKey: ['categorias'],
    queryFn: async () => {
      const { data } = await categoriasApi.listar();
      // Ordenar por campo `orden` del backend
      return data.sort((a, b) => a.orden - b.orden);
    },
    refetchInterval: 60_000,
    staleTime: 30_000,
  });
}

/**
 * Crear nueva categoría.
 * Invalida el prefijo para actualizar listados.
 */
export function useCrearCategoria() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CategoriaRequest) => categoriasApi.crear(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['categorias'], exact: false });
    },
    onError: (error: Error) => {
      console.error('[useCrearCategoria] Error al crear categoría:', error);
    },
  });
}

/**
 * Editar categoría existente.
 * Invalida categorías y productos (el cambio de categoría puede afectar la UI de productos).
 */
export function useEditarCategoria() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, ...data }: { id: string } & CategoriaRequest) =>
      categoriasApi.editar(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['categorias'], exact: false });
      queryClient.invalidateQueries({ queryKey: ['productos'], exact: false });
    },
    onError: (error: Error) => {
      console.error('[useEditarCategoria] Error al editar categoría:', error);
    },
  });
}

/**
 * Eliminar categoría.
 * Invalida categorías y productos.
 */
export function useEliminarCategoria() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => categoriasApi.eliminar(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['categorias'], exact: false });
      queryClient.invalidateQueries({ queryKey: ['productos'], exact: false });
    },
    onError: (error: Error) => {
      console.error('[useEliminarCategoria] Error al eliminar categoría:', error);
    },
  });
}
