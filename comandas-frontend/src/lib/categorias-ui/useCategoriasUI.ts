import { useEffect } from 'react';
import { useCategoriasStore } from './store';
import type { CategoriaUI } from './types';

// ─── Categoría virtual "Todos" ─────────────────────────────────────────────────

const CATEGORIA_TODOS: CategoriaUI = {
  id: '__todos__',
  nombre: 'Todos',
  colorBase: '',
  colorDisplay: '#FFFFFF',
  esExtra: false,
  orden: -1,
};

// ─── Hook ──────────────────────────────────────────────────────────────────────

/**
 * Hook principal para consumir y gestionar categorías visuales.
 *
 * Carga automáticamente desde Tauri Store al montar (si no se cargó antes).
 *
 * Devuelve:
 * - Estado: categorías, loading, error
 * - Queries: búsqueda por color, listados filtrados
 * - Mutaciones: crear, editar, eliminar, reordenar
 *
 * Reemplazo directo de:
 * - `CATEGORIAS` (constante) → `categorias` / `categoriasConTodos`
 * - `CATEGORIAS_ASIGNABLES` → `categoriasAsignables`
 * - `nombreCategoria(hex)` → `nombreCategoria(hex)`
 *
 * @example
 * const { categorias, crearCategoria, isLoading } = useCategoriasUI();
 */
export function useCategoriasUI() {
  const store = useCategoriasStore();

  // Cargar al montar (idempotente — no recarga si ya cargó)
  useEffect(() => {
    if (!store.cargado) {
      store.cargar();
    }
  }, [store.cargado, store.cargar]);

  return {
    // ── Estado ──
    categorias: store.categorias,
    isLoading: !store.cargado,
    error: store.error,

    // ── Queries ──

    /**
     * Solo categorías asignables a productos (para selectores).
     * Equivalente al viejo `CATEGORIAS_ASIGNABLES`.
     */
    categoriasAsignables: store.categoriasAsignables(),

    /**
     * Categorías con "Todos" al inicio (para filtros en POS y admin).
     * Equivalente al viejo `CATEGORIAS`.
     */
    categoriasConTodos: [CATEGORIA_TODOS, ...store.categorias],

    /** Buscar categoría por `colorHex` del producto */
    categoriaDeColor: store.categoriaDeColor,

    /**
     * Nombre legible de la categoría dado un `colorHex`.
     * Reemplazo directo de `nombreCategoria()` de `lib/categorias.ts`.
     */
    nombreCategoria: (colorHex: string): string => {
      return store.categoriaDeColor(colorHex)?.nombre ?? 'Sin categoría';
    },

    // ── Mutaciones ──
    crearCategoria: store.crearCategoria,
    editarCategoria: store.editarCategoria,
    eliminarCategoria: store.eliminarCategoria,
    reordenarCategorias: store.reordenarCategorias,
  };
}
