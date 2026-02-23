import { create } from 'zustand';
import type { CategoriaUI, CategoriasStoreSchema } from './types';
import { SCHEMA_VERSION } from './types';
import { CATEGORIAS_DEFAULT } from './defaults';
import { crearAdapter } from './persistence';
import { validarYSanitizar, crearDesdeDefaults } from './migrations';

// ─── Tipos del store ───────────────────────────────────────────────────────────

interface CategoriasState {
  // ── Estado ──
  categorias: CategoriaUI[];
  ordenProductos: Record<string, string[]>;
  cargado: boolean;
  error: string | null;

  // ── Lifecycle ──
  cargar: () => Promise<void>;

  // ── CRUD de categorías ──
  crearCategoria: (
    cat: Omit<CategoriaUI, 'id' | 'orden'>
  ) => Promise<CategoriaUI>;
  editarCategoria: (
    id: string,
    cambios: Partial<
      Pick<CategoriaUI, 'nombre' | 'colorBase' | 'colorDisplay' | 'esExtra'>
    >
  ) => Promise<void>;
  eliminarCategoria: (id: string) => Promise<void>;
  reordenarCategorias: (ids: string[]) => Promise<void>;

  // ── Orden de productos ──
  setOrdenProductos: (
    categoriaId: string,
    productIds: string[]
  ) => Promise<void>;
  limpiarProductoDeOrden: (productoId: string) => void;

  // ── Queries ──
  categoriaDeColor: (colorHex: string) => CategoriaUI | undefined;
  categoriasAsignables: () => CategoriaUI[];
}

// ─── Adapter singleton ─────────────────────────────────────────────────────────

const adapter = crearAdapter();

// ─── Store ─────────────────────────────────────────────────────────────────────

export const useCategoriasStore = create<CategoriasState>((set, get) => ({
  categorias: [],
  ordenProductos: {},
  cargado: false,
  error: null,

  // ── Cargar desde persistencia ──

  cargar: async () => {
    try {
      const raw = await adapter.load();
      const data = raw ? validarYSanitizar(raw) : crearDesdeDefaults();

      set({
        categorias: data.categorias.sort((a, b) => a.orden - b.orden),
        ordenProductos: data.ordenProductos,
        cargado: true,
        error: null,
      });

      // Persistir si era primera vez o se migró
      if (!raw || (raw as CategoriasStoreSchema).schemaVersion !== SCHEMA_VERSION) {
        await adapter.save(data);
      }
    } catch (e) {
      console.error('[CategoríasUI] Error al cargar:', e);
      set({
        categorias: CATEGORIAS_DEFAULT,
        ordenProductos: {},
        cargado: true,
        error: String(e),
      });
    }
  },

  // ── Crear categoría ──

  crearCategoria: async (cat) => {
    const { categorias } = get();

    // Validar color único
    const colorUpper = cat.colorBase.toUpperCase();
    const conflicto = categorias.find(
      (c) => c.colorBase.toUpperCase() === colorUpper
    );
    if (conflicto) {
      throw new Error(
        `El color ${cat.colorBase} ya está asignado a "${conflicto.nombre}"`
      );
    }

    const nueva: CategoriaUI = {
      ...cat,
      id: crypto.randomUUID(),
      orden: categorias.length,
    };

    const updated = [...categorias, nueva];
    set({ categorias: updated });
    await persistir(get);

    return nueva;
  },

  // ── Editar categoría ──

  editarCategoria: async (id, cambios) => {
    const { categorias } = get();

    // Validar color único si cambió
    if (cambios.colorBase) {
      const colorUpper = cambios.colorBase.toUpperCase();
      const conflicto = categorias.find(
        (c) => c.id !== id && c.colorBase.toUpperCase() === colorUpper
      );
      if (conflicto) {
        throw new Error(
          `El color ${cambios.colorBase} ya está asignado a "${conflicto.nombre}"`
        );
      }
    }

    const updated = categorias.map((c) =>
      c.id === id ? { ...c, ...cambios } : c
    );
    set({ categorias: updated });
    await persistir(get);
  },

  // ── Eliminar categoría ──

  eliminarCategoria: async (id) => {
    const { categorias, ordenProductos } = get();

    const updated = categorias
      .filter((c) => c.id !== id)
      .map((c, i) => ({ ...c, orden: i })); // Reindexar

    // Limpiar orden de productos de la categoría eliminada
    const { [id]: _, ...restOrden } = ordenProductos;

    set({ categorias: updated, ordenProductos: restOrden });
    await persistir(get);
  },

  // ── Reordenar categorías ──

  reordenarCategorias: async (ids) => {
    const { categorias } = get();
    const mapa = new Map(categorias.map((c) => [c.id, c]));

    const updated = ids
      .map((id, i) => {
        const cat = mapa.get(id);
        return cat ? { ...cat, orden: i } : null;
      })
      .filter((c): c is CategoriaUI => c !== null);

    set({ categorias: updated });
    await persistir(get);
  },

  // ── Orden de productos dentro de categoría ──

  setOrdenProductos: async (categoriaId, productIds) => {
    const { ordenProductos } = get();
    set({
      ordenProductos: { ...ordenProductos, [categoriaId]: productIds },
    });
    await persistir(get);
  },

  limpiarProductoDeOrden: (productoId) => {
    const { ordenProductos } = get();
    const updated = Object.fromEntries(
      Object.entries(ordenProductos).map(([catId, ids]) => [
        catId,
        ids.filter((id) => id !== productoId),
      ])
    );
    set({ ordenProductos: updated });
    // No persistir inmediatamente — cleanup lazy
  },

  // ── Queries ──

  categoriaDeColor: (colorHex) => {
    if (!colorHex) return undefined;
    const upper = colorHex.toUpperCase();
    return get().categorias.find(
      (c) => c.colorBase.toUpperCase() === upper
    );
  },

  categoriasAsignables: () => get().categorias,
}));

// ─── Helpers internos ──────────────────────────────────────────────────────────

async function persistir(get: () => CategoriasState) {
  const { categorias, ordenProductos } = get();
  await adapter.save({
    schemaVersion: SCHEMA_VERSION,
    categorias,
    ordenProductos,
  });
}
