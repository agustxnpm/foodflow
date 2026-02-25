import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { productosApi } from '../api/productosApi';
import type { ProductoResponse, ProductoRequest, VarianteRequest } from '../types';

/**
 * Lista productos filtrados por categoriaId.
 * queryKey: ['productos', categoriaId] para invalidación granular.
 * Polling cada 60s: los productos/promociones rara vez cambian
 * durante el servicio, pero el badge de "Promo Activa" debe
 * actualizarse eventualmente sin requerir F5.
 */
export function useProductos(categoriaId: string | null = null) {
  return useQuery<ProductoResponse[]>({
    queryKey: ['productos', categoriaId],
    queryFn: async () => {
      const { data } = await productosApi.listar(categoriaId);
      return data;
    },
    refetchInterval: 60_000,
  });
}

/**
 * Lista productos extras reales del catálogo.
 * queryKey: ['productos', 'extras'] para invalidación independiente.
 *
 * Filtro estricto basado en el dominio:
 * 1. El producto debe pertenecer a una categoría con esCategoriaExtra === true
 * 2. El producto debe estar activo
 *
 * Esto excluye discos estructurales y cualquier producto que no pertenezca
 * a una categoría de extras. El dominio define qué es un extra, no el flag aislado.
 *
 * Requiere que se pasen las categorías ya cargadas para resolver el flag.
 */
export function useExtras(categorias: import('../../categorias/types').CategoriaResponse[] = []) {
  // IDs de categorías que son de extras
  const categoriasExtrasIds = new Set(
    categorias.filter((c) => c.esCategoriaExtra).map((c) => c.id)
  );

  return useQuery<ProductoResponse[]>({
    queryKey: ['productos', 'extras', [...categoriasExtrasIds].sort().join(',')],
    queryFn: async () => {
      const { data } = await productosApi.listar(null);
      return data.filter(
        (p) => p.activo && p.categoriaId != null && categoriasExtrasIds.has(p.categoriaId)
      );
    },
    // Solo activar cuando tengamos categorías resueltas
    enabled: categoriasExtrasIds.size > 0,
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

// ─── Variantes ────────────────────────────────────────────────────────────────

/**
 * Lista las variantes de un producto (hermanas del mismo grupo).
 * Solo se activa cuando productoId está definido.
 * No aplica polling porque se usa en el modal de gestión (corta vida).
 */
export function useVariantes(productoId?: string) {
  return useQuery<ProductoResponse[]>({
    queryKey: ['variantes', productoId],
    queryFn: async () => {
      const { data } = await productosApi.listarVariantes(productoId!);
      return data;
    },
    enabled: !!productoId,
  });
}

/**
 * Crear una variante de un producto base.
 * Invalida productos y variantes para refrescar ambas vistas.
 */
export function useCrearVariante() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ productoBaseId, ...data }: { productoBaseId: string } & VarianteRequest) =>
      productosApi.crearVariante(productoBaseId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['productos'], exact: false });
      queryClient.invalidateQueries({ queryKey: ['variantes'], exact: false });
      queryClient.invalidateQueries({ queryKey: ['producto'], exact: false });
    },
    onError: (error: Error) => {
      console.error('[useCrearVariante] Error al crear variante:', error);
    },
  });
}
