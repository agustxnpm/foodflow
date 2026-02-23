/**
 * Categorías de productos del local — ÚNICA fuente de verdad.
 *
 * Cada categoría se identifica por su `color` (hex exacto almacenado en el backend
 * como `color_hex`). El campo `hex` es el color de visualización en el UI
 * (puede diferir para mejor legibilidad sobre fondos oscuros).
 *
 * Este archivo es importado por:
 *  - ListaCategorias (POS): navegación lateral
 *  - VistaCatalogo (Admin): pills de filtro y agrupamiento
 *  - ProductoModal (Admin): selector de categoría al crear/editar
 *
 * TODO: Eventualmente esto viene del backend como configuración del tenant.
 */

export interface CategoriaColor {
  /** Valor almacenado en BD (color_hex). null = "Todos" (solo para filtros) */
  color: string | null;
  /** Nombre legible de la categoría */
  label: string;
  /** Hex para renderizar el indicador visual en el UI */
  hex: string;
  /** Si esta categoría representa extras (huevo, queso, disco, etc.) */
  esExtra?: boolean;
}

/**
 * Categorías disponibles en el local.
 *
 * - La primera entrada (color: null) es el filtro "Todos", NO es asignable a un producto.
 * - Las demás son categorías reales que se persisten como `color_hex` en el backend.
 */
export const CATEGORIAS: CategoriaColor[] = [
  { color: null,      label: 'Todos',         hex: '#FFFFFF' },
  { color: '#FF0000', label: 'Hamburguesas',  hex: '#FF0000' },
  { color: '#0000FF', label: 'Bebidas',       hex: '#3B82F6' },
  { color: '#00FF00', label: 'Ensaladas',     hex: '#22C55E' },
  { color: '#FFA500', label: 'Postres',       hex: '#F97316' },
  { color: '#800080', label: 'Pizzas',        hex: '#A855F7' },
  { color: '#FFFF00', label: 'Extras',        hex: '#EAB308', esExtra: true },
];

/**
 * Solo las categorías asignables a productos (excluye "Todos").
 * Usar en selectores de creación/edición.
 */
export const CATEGORIAS_ASIGNABLES = CATEGORIAS.filter(
  (c): c is CategoriaColor & { color: string } => c.color !== null
);

/**
 * Nombre legible de categoría a partir del colorHex del producto.
 * Comparación case-insensitive para robustez ante datos legacy.
 */
export function nombreCategoria(colorHex: string): string {
  const upper = colorHex?.toUpperCase();
  return (
    CATEGORIAS.find((c) => c.color?.toUpperCase() === upper)?.label ??
    'Sin categoría'
  );
}
