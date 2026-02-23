import { useMemo, useCallback } from 'react';
import { useCategoriasStore } from './store';
import type { ProductoResponse } from '../../features/catalogo/types';

// ─── Tipos ─────────────────────────────────────────────────────────────────────

export interface GrupoProductos {
  categoriaId: string;
  label: string;
  hex: string;
  esExtra: boolean;
  productos: ProductoResponse[];
}

// ─── Hook ──────────────────────────────────────────────────────────────────────

/**
 * Hook para agrupar y ordenar productos dentro de categorías.
 *
 * Combina los productos del backend con:
 * - Las categorías del Tauri Store (orden de grupos)
 * - El orden personalizado de productos por categoría
 *
 * Los productos sin categoría se agrupan al final en "Sin categoría".
 *
 * @param productos - Lista de productos del backend
 * @returns `grupos` agrupados + `reordenarEn` para drag & drop
 *
 * @example
 * const { grupos, reordenarEn } = useOrdenProductos(productos);
 */
export function useOrdenProductos(productos: ProductoResponse[]) {
  const { categorias, ordenProductos, setOrdenProductos } =
    useCategoriasStore();

  /** Productos agrupados por categoría, respetando orden personalizado */
  const grupos = useMemo((): GrupoProductos[] => {
    const porCategoria = new Map<string, ProductoResponse[]>();
    const sinCategoria: ProductoResponse[] = [];

    // 1. Clasificar cada producto por su colorHex → categoría
    productos.forEach((p) => {
      const hex = p.colorHex?.toUpperCase();
      const cat = hex
        ? categorias.find((c) => c.colorBase.toUpperCase() === hex)
        : null;

      if (cat) {
        const arr = porCategoria.get(cat.id) ?? [];
        arr.push(p);
        porCategoria.set(cat.id, arr);
      } else {
        sinCategoria.push(p);
      }
    });

    // 2. Construir grupos ordenados por `orden` de categoría
    const resultado = [...categorias]
      .sort((a, b) => a.orden - b.orden)
      .filter((c) => porCategoria.has(c.id))
      .map((c) => ({
        categoriaId: c.id,
        label: c.nombre,
        hex: c.colorDisplay,
        esExtra: c.esExtra,
        productos: aplicarOrdenPersonalizado(
          porCategoria.get(c.id)!,
          ordenProductos[c.id]
        ),
      }));

    // 3. Productos huérfanos al final
    if (sinCategoria.length > 0) {
      resultado.push({
        categoriaId: '__sin_categoria__',
        label: 'Sin categoría',
        hex: '#6B7280',
        esExtra: false,
        productos: sinCategoria.sort((a, b) =>
          a.nombre.localeCompare(b.nombre, 'es')
        ),
      });
    }

    return resultado;
  }, [productos, categorias, ordenProductos]);

  /** Reordena los productos dentro de una categoría (drag & drop) */
  const reordenarEn = useCallback(
    (categoriaId: string, productIds: string[]) => {
      setOrdenProductos(categoriaId, productIds);
    },
    [setOrdenProductos]
  );

  return { grupos, reordenarEn };
}

// ─── Helper ────────────────────────────────────────────────────────────────────

/**
 * Aplica un orden personalizado a los productos.
 *
 * - Los productos que están en `orden` se muestran primero, en ese orden.
 * - Los productos nuevos (no están en `orden`) van al final, alfabéticamente.
 * - IDs en `orden` que ya no existen se ignoran (cleanup lazy).
 */
function aplicarOrdenPersonalizado(
  productos: ProductoResponse[],
  orden: string[] | undefined
): ProductoResponse[] {
  if (!orden || orden.length === 0) {
    // Sin orden personalizado → alfabético por nombre
    return [...productos].sort((a, b) =>
      a.nombre.localeCompare(b.nombre, 'es')
    );
  }

  const mapa = new Map(productos.map((p) => [p.id, p]));
  const ordenados: ProductoResponse[] = [];
  const vistos = new Set<string>();

  // Primero: los que tienen orden definido
  orden.forEach((id) => {
    const p = mapa.get(id);
    if (p) {
      ordenados.push(p);
      vistos.add(id);
    }
  });

  // Después: los nuevos (sin orden), alfabéticamente
  productos
    .filter((p) => !vistos.has(p.id))
    .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'))
    .forEach((p) => ordenados.push(p));

  return ordenados;
}
