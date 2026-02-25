/**
 * Tipos de dominio para el módulo Categorías.
 *
 * Refleja los DTOs del backend: CategoriaRequest, CategoriaResponse.
 * Las categorías ahora son entidades de dominio persistidas en el backend,
 * no pseudocategorías visuales del Tauri Store.
 *
 * @see backend: com.agustinpalma.comandas.application.dto
 */

// ─── Response ─────────────────────────────────────────────────────────────────

/**
 * DTO de salida para categorías.
 * Refleja CategoriaResponse del backend.
 */
export interface CategoriaResponse {
  /** UUID como string */
  id: string;
  nombre: string;
  colorHex: string;
  /** Si los productos de esta categoría admiten variantes (ej: hamburguesas con discos) */
  admiteVariantes: boolean;
  /** Si esta categoría agrupa productos extras (huevo, queso, medallón, etc.) */
  esCategoriaExtra: boolean;
  /** Posición en la lista (0-based). Define el orden visual */
  orden: number;
}

// ─── Request ──────────────────────────────────────────────────────────────────

/**
 * DTO de entrada para crear o editar una categoría.
 * Refleja CategoriaRequest del backend.
 */
export interface CategoriaRequest {
  nombre: string;
  colorHex?: string;
  admiteVariantes?: boolean;
  esCategoriaExtra?: boolean;
  orden?: number;
}
