import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { productosApi } from '../api/productosApi';
import type { ProductoResponse, ProductoRequest } from '../types';

/**
 * Lista productos filtrados por color (categoría visual).
 * queryKey: ['productos', color] para invalidación granular.
 */
export function useProductos(color: string | null = null) {
  return useQuery<ProductoResponse[]>({
    queryKey: ['productos', color],
    queryFn: async () => {
      const { data } = await productosApi.listar(color);
      return data;
    },
  });
}

/**
 * Consulta un producto individual por ID.
 * Habilitado solo cuando el ID está definido.
 */
export function useProducto(id?: string) {
  return useQuery<ProductoResponse>({
    queryKey: ['producto', id],
    queryFn: async () => {
      const { data } = await productosApi.consultar(id!);
      return data;
    },
    enabled: !!id,
  });
}

/**
 * HU-19: Crear nuevo producto en el catálogo.
 * Invalida el prefijo para actualizar listados y filtros.
 */
export function useCrearProducto() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (data: ProductoRequest) => productosApi.crear(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['productos'], exact: false });
    },
    onError: (error: Error) => {
      console.error('[useCrearProducto] Error al crear producto:', error);
    },
  });
}

/**
 * HU-19: Editar producto existente.
 * Invalida prefijo completo para actualizar detalles y listados.
 */
export function useEditarProducto() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ id, ...data }: { id: string } & Partial<ProductoRequest>) =>
      productosApi.editar(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['productos'], exact: false });
      queryClient.invalidateQueries({ queryKey: ['producto'], exact: false });
    },
    onError: (error: Error) => {
      console.error('[useEditarProducto] Error al editar producto:', error);
    },
  });
}

/**
 * HU-19: Eliminar producto del catálogo.
 * Invalida todos los listados de productos.
 */
export function useEliminarProducto() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (id: string) => productosApi.eliminar(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['productos'], exact: false });
    },
    onError: (error: Error) => {
      console.error('[useEliminarProducto] Error al eliminar producto:', error);
    },
  });
}

/**
 * HU-22: Ajustar stock de productos.
 * CRÍTICO: Invalida todo el prefijo para actualizar disponibilidad en toma de pedidos.
 */
export function useAjustarStock() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ id, ...data }: { id: string; cantidad: number; tipo: string }) =>
      productosApi.ajustarStock(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['productos'], exact: false });
      queryClient.invalidateQueries({ queryKey: ['producto'], exact: false });
    },
    onError: (error: Error) => {
      console.error('[useAjustarStock] Error al ajustar stock:', error);
    },
  });
}
