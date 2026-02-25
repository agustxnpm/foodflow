/**
 * Utilidades para decidir el comportamiento al tocar un producto en el POS.
 *
 * Estrategia: El frontend NO interpreta el negocio. Todas las decisiones
 * se basan en flags explícitos del backend:
 *
 * - `esExtra` → el producto es un extra, nunca se agrega desde la grilla
 * - `requiereConfiguracion` → flag explícito del backend
 * - `permiteExtras` → si admite agregados/extras
 *
 * Las heurísticas antiguas (esBebida, resolveCategoria por colorHex,
 * CATEGORIAS_BEBIDA) fueron eliminadas. El dominio manda.
 */

import type { ProductoResponse } from '../../catalogo/types';
import type { CategoriaResponse } from '../../categorias/types';

// ─── permiteAbrirModal ─────────────────────────────────────────────────────────

/**
 * Determina si el POS debe abrir el modal de configuración (observaciones + extras)
 * antes de agregar el producto al pedido.
 *
 * La decisión se basa exclusivamente en flags del backend:
 * 1. esExtra === true → nunca abre modal (un extra no tiene sub-extras)
 * 2. requiereConfiguracion === false → no abre modal
 * 3. permiteExtras === false → no abre modal (sin extras, sin sentido)
 * 4. esCategoriaExtra en la categoría → no abre modal (es un extra)
 *
 * Si todas las condiciones pasan → abre modal.
 *
 * @param producto - ProductoResponse del backend
 * @param categorias - Lista de categorías del backend (para verificar esCategoriaExtra)
 * @returns true si el modal debe abrirse
 */
export function permiteAbrirModal(
  producto: ProductoResponse,
  categorias: CategoriaResponse[] = []
): boolean {
  // Un extra nunca abre modal
  if (producto.esExtra) return false;

  // Si la categoría del producto es de extras, no abre modal
  if (producto.categoriaId) {
    const categoria = categorias.find((c) => c.id === producto.categoriaId);
    if (categoria?.esCategoriaExtra) return false;
  }

  // requiereConfiguracion debe ser true (default true si no viene)
  if (producto.requiereConfiguracion === false) return false;

  // permiteExtras: si es false, no tiene sentido abrir modal
  if (producto.permiteExtras === false) return false;

  return true;
}
