/**
 * Tipos del módulo de pseudocategorías visuales.
 *
 * Las categorías NO existen en el backend. Son un concepto 100% de presentación
 * persistido localmente en Tauri Store (desktop) o en memoria (dev/browser).
 *
 * La relación producto ↔ categoría se determina por:
 *   CategoriaUI.colorBase === Producto.colorHex
 */

// ─── Categoría visual ──────────────────────────────────────────────────────────

/**
 * Categoría visual persistente.
 *
 * Representa una agrupación de productos definida por el usuario.
 * La pertenencia se determina por `colorBase === producto.colorHex`.
 */
export interface CategoriaUI {
  /** UUID estable. No cambia aunque cambie nombre o color. */
  id: string;

  /** Nombre editable por el usuario. Ej: "Hamburguesas", "Lo más pedido" */
  nombre: string;

  /**
   * Hex canónico que se persiste en el backend como `color_hex`.
   * Es el JOIN lógico entre categoría y producto.
   * DEBE ser único entre todas las categorías.
   * Formato: '#RRGGBB' (uppercase, 7 chars).
   */
  colorBase: string;

  /**
   * Hex para renderizar en UI. Puede diferir de colorBase
   * para mejor legibilidad sobre fondos oscuros.
   * Si no se define, se usa colorBase.
   */
  colorDisplay: string;

  /** Si los productos de esta categoría son extras (huevo, queso, etc.) */
  esExtra: boolean;

  /** Posición en la lista (0-based). Define el orden visual. */
  orden: number;
}

// ─── Esquema persistido ────────────────────────────────────────────────────────

/**
 * Esquema completo del store de categorías.
 * Persistido como JSON en Tauri Store (`categorias-ui.json`).
 */
export interface CategoriasStoreSchema {
  /** Versión del esquema para migraciones futuras */
  schemaVersion: number;

  /** Categorías definidas por el usuario, ordenadas por `orden` */
  categorias: CategoriaUI[];

  /**
   * Orden personalizado de productos dentro de cada categoría.
   * Clave: categoriaId (UUID estable).
   * Valor: array de productoId (UUID) en el orden deseado.
   *
   * Los productos que no aparecen en el array van al final,
   * ordenados alfabéticamente (fallback).
   */
  ordenProductos: Record<string, string[]>;
}

/** Versión actual del esquema */
export const SCHEMA_VERSION = 1;
