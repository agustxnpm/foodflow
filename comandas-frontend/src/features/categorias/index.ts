/**
 * Módulo de Categorías — entidades de dominio del backend.
 *
 * Reemplaza completamente el antiguo módulo `lib/categorias-ui`
 * (pseudocategorías del Tauri Store) y `lib/categorias.ts` (constantes hardcodeadas).
 *
 * Las categorías ahora son entidades persistidas en el backend con:
 * - CRUD vía API REST
 * - Flags de negocio: admiteVariantes, esCategoriaExtra
 * - Orden configurable
 *
 * @example
 * import { useCategorias } from '@/features/categorias';
 * import type { CategoriaResponse } from '@/features/categorias';
 */

// Tipos
export type { CategoriaResponse, CategoriaRequest } from './types';

// Hooks — API pública principal
export {
  useCategorias,
  useCrearCategoria,
  useEditarCategoria,
  useEliminarCategoria,
} from './hooks/useCategorias';

// API — para uso directo si se necesita
export { categoriasApi } from './api/categoriasApi';
