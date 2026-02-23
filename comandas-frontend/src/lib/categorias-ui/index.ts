/**
 * Módulo de pseudocategorías visuales persistentes.
 *
 * Exports públicos para el resto de la app.
 * Los internos (migrations, persistence, store) NO se exponen.
 *
 * @example
 * import { useCategoriasUI, useOrdenProductos } from '@/lib/categorias-ui';
 * import type { CategoriaUI } from '@/lib/categorias-ui';
 */

// Tipos
export type { CategoriaUI, CategoriasStoreSchema } from './types';
export { SCHEMA_VERSION } from './types';

// Defaults (para tests o reset manual)
export { CATEGORIAS_DEFAULT } from './defaults';

// Hooks — API pública principal
export { useCategoriasUI } from './useCategoriasUI';
export { useOrdenProductos } from './useOrdenProductos';
export type { GrupoProductos } from './useOrdenProductos';
