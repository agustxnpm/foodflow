/**
 * Utilidades para resolver la categoría y decidir si abrir el modal
 * de configuración de un producto antes de agregarlo al pedido.
 *
 * Estrategia de resolución:
 * 1. Si el backend provee `categoria` → se usa directamente.
 * 2. Fallback: se consulta el store de pseudocategorías visuales (Tauri Store)
 *    buscando por `colorHex` del producto.
 *
 * Esto garantiza retrocompatibilidad: si el backend no está actualizado
 * (categoria = null/undefined), la experiencia sigue funcionando
 * gracias al store local de pseudocategorías.
 */

import type { ProductoResponse } from '../../catalogo/types';
import type { CategoriaUI } from '../../../lib/categorias-ui';

// ─── Constantes ────────────────────────────────────────────────────────────────

/**
 * Nombres de categoría que se consideran "bebida" para la lógica de modal.
 * Comparación case-insensitive. Soporta tanto el campo `categoria` del backend
 * como el `nombre` de la pseudocategoría del store local.
 */
const CATEGORIAS_BEBIDA = ['bebida', 'bebidas'];

// ─── resolveCategoria ──────────────────────────────────────────────────────────

/**
 * Resuelve el nombre de categoría de un producto.
 *
 * Prioridad:
 * 1. `producto.categoria` (campo del backend, si presente)
 * 2. Nombre de la pseudocategoría del store local (match por colorHex)
 *
 * @param producto  - ProductoResponse del backend
 * @param buscarCategoriaPorColor - Función del store: colorHex → CategoriaUI | undefined
 * @returns nombre de categoría normalizado a lowercase, o undefined si no se pudo resolver
 */
export function resolveCategoria(
  producto: ProductoResponse,
  buscarCategoriaPorColor: (colorHex: string) => CategoriaUI | undefined
): string | undefined {
  // 1. Campo del backend (fuente preferida)
  if (producto.categoria) {
    return producto.categoria.trim().toLowerCase();
  }

  // 2. Fallback: pseudocategoría visual del store local
  if (producto.colorHex) {
    const categoriaUI = buscarCategoriaPorColor(producto.colorHex);
    if (categoriaUI) {
      return categoriaUI.nombre.trim().toLowerCase();
    }
  }

  return undefined;
}

// ─── esBebida ──────────────────────────────────────────────────────────────────

/**
 * Determina si la categoría resuelta corresponde a una bebida.
 */
export function esBebida(categoriaResuelta: string | undefined): boolean {
  if (!categoriaResuelta) return false;
  return CATEGORIAS_BEBIDA.includes(categoriaResuelta);
}

// ─── permiteAbrirModal ─────────────────────────────────────────────────────────

/**
 * Determina si el POS debe abrir el modal de configuración (observaciones + extras)
 * antes de agregar el producto al pedido.
 *
 * Condiciones para abrir el modal:
 * 1. requiereConfiguracion === true (flag del backend; default true)
 * 2. permiteExtras !== false (si undefined → default true para retrocompatibilidad)
 * 3. esExtra === false (un extra no puede tener sub-extras)
 * 4. NO es una bebida (resuelto por categoria o fallback store)
 *
 * Si cualquiera de estas condiciones falla → no abrir modal,
 * agregar directamente al pedido con cantidad 1.
 *
 * @param producto - ProductoResponse del backend
 * @param buscarCategoriaPorColor - Función del store: colorHex → CategoriaUI | undefined
 * @returns true si el modal debe abrirse
 */
export function permiteAbrirModal(
  producto: ProductoResponse,
  buscarCategoriaPorColor: (colorHex: string) => CategoriaUI | undefined
): boolean {
  // Un extra nunca abre modal (ya implementado, ahora explícito)
  if (producto.esExtra) return false;

  // requiereConfiguracion debe ser true (default true si no viene)
  if (producto.requiereConfiguracion === false) return false;

  // permiteExtras: si es false, no tiene sentido abrir modal
  // Si undefined → default true (backend legacy que no envía el campo)
  if (producto.permiteExtras === false) return false;

  // Bebidas: nunca abren modal de configuración
  const categoria = resolveCategoria(producto, buscarCategoriaPorColor);
  if (esBebida(categoria)) return false;

  return true;
}
