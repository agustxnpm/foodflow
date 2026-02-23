import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { productosApi } from '../api/productosApi';
import type { ProductoResponse, ProductoRequest } from '../types';

/**
 * Lista productos filtrados por color (categoría visual).
 * queryKey: ['productos', color] para invalidación granular.
 * Polling cada 60s: los productos/promociones rara vez cambian
 * durante el servicio, pero el badge de "Promo Activa" debe
 * actualizarse eventualmente sin requerir F5.
 */
export function useProductos(color: string | null = null) {
  return useQuery<ProductoResponse[]>({
    queryKey: ['productos', color],
    queryFn: async () => {
      const { data } = await productosApi.listar(color);
      return data;
    },
    refetchInterval: 60_000,
  });
}

/**
 * Lista productos marcados como extra (huevo, queso, disco de carne, etc.).
 * queryKey: ['productos', 'extras'] para invalidación independiente.
 * Filtra del catálogo general los productos con esExtra === true.
 */
export function useExtras() {
  return useQuery<ProductoResponse[]>({
    queryKey: ['productos', 'extras'],
    queryFn: async () => {
      const { data } = await productosApi.listar(null);
      return data.filter((p) => p.esExtra && p.activo);
    },
    refetchInterval: 60_000,
  });
}

/**
 * Consulta un producto individual por ID.
 * Habilitado solo cuando el ID está definido.
 * Polling cada 60s para capturar cambios de precio o promos.
 */
export function useProducto(id?: string) {
  return useQuery<ProductoResponse>({
    queryKey: ['producto', id],
    queryFn: async () => {
      const { data } = await productosApi.consultar(id!);
      return data;
    },
    enabled: !!id,
    refetchInterval: 60_000,
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
    mutationFn: ({ id, ...data }: { id: string; cantidad: number; tipo: string; motivo: string }) =>
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
