import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { productosApi } from '../api/productosApi';

export function useProductos(color = null) {
  return useQuery({
    queryKey: ['productos', color],
    queryFn: () => productosApi.listar(color),
  });
}

export function useProducto(id) {
  return useQuery({
    queryKey: ['producto', id],
    queryFn: () => productosApi.consultar(id),
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
    mutationFn: productosApi.crear,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['productos'], exact: false });
    },
    onError: (error) => {
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
    mutationFn: ({ id, ...data }) => productosApi.editar(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['productos'], exact: false });
      queryClient.invalidateQueries({ queryKey: ['producto'], exact: false });
    },
    onError: (error) => {
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
    mutationFn: productosApi.eliminar,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['productos'], exact: false });
    },
    onError: (error) => {
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
    mutationFn: ({ id, ...data }) => productosApi.ajustarStock(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['productos'], exact: false });
      queryClient.invalidateQueries({ queryKey: ['producto'], exact: false });
    },
    onError: (error) => {
      console.error('[useAjustarStock] Error al ajustar stock:', error);
    },
  });
}
