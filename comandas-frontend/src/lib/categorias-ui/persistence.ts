import type { CategoriasStoreSchema } from './types';

// ─── Interfaz de persistencia ──────────────────────────────────────────────────

/**
 * Contrato de persistencia para categorías.
 *
 * Permite intercambiar Tauri Store (desktop) por un adapter
 * in-memory (dev/browser) sin cambiar la lógica de negocio.
 */
export interface CategoriasStorePersistence {
  load(): Promise<CategoriasStoreSchema | null>;
  save(data: CategoriasStoreSchema): Promise<void>;
}

// ─── Tauri Store Adapter ───────────────────────────────────────────────────────

/**
 * Adapter para `@tauri-apps/plugin-store`.
 *
 * Usa LazyStore con autoSave de 300ms (debounce).
 * El archivo se persiste en el directorio de datos de la app:
 *   - Linux: ~/.local/share/com.meiser.foodflow/categorias-ui.json
 *   - macOS: ~/Library/Application Support/com.meiser.foodflow/categorias-ui.json
 *   - Windows: %APPDATA%/com.meiser.foodflow/categorias-ui.json
 */
class TauriStoreAdapter implements CategoriasStorePersistence {
  private storePromise: Promise<unknown> | null = null;

  private async getStore() {
    if (!this.storePromise) {
      this.storePromise = import('@tauri-apps/plugin-store').then(
        ({ LazyStore }) => new LazyStore('categorias-ui.json', { defaults: {}, autoSave: 300 })
      );
    }
    return this.storePromise as Promise<{
      get<T>(key: string): Promise<T | null>;
      set(key: string, value: unknown): Promise<void>;
      save(): Promise<void>;
    }>;
  }

  async load(): Promise<CategoriasStoreSchema | null> {
    try {
      const store = await this.getStore();
      const data = await store.get<CategoriasStoreSchema>('config');
      return data ?? null;
    } catch (e) {
      console.error('[CategoríasUI] Error al leer Tauri Store:', e);
      return null;
    }
  }

  async save(data: CategoriasStoreSchema): Promise<void> {
    try {
      const store = await this.getStore();
      await store.set('config', data);
      // autoSave (300ms debounce) se encarga de escribir a disco.
      // Para operaciones críticas, el store.ts llama saveForzado().
    } catch (e) {
      console.error('[CategoríasUI] Error al escribir Tauri Store:', e);
    }
  }

  /**
   * Fuerza escritura inmediata a disco.
   * Usar solo para operaciones destructivas (eliminar, cambiar color).
   */
  async saveForzado(data: CategoriasStoreSchema): Promise<void> {
    try {
      const store = await this.getStore();
      await store.set('config', data);
      await store.save();
    } catch (e) {
      console.error('[CategoríasUI] Error en save forzado:', e);
    }
  }
}

// ─── In-Memory Adapter ─────────────────────────────────────────────────────────

/**
 * Adapter in-memory para desarrollo en browser (sin Tauri).
 *
 * Las categorías se inicializan desde defaults y se pierden al refrescar.
 * Permite desarrollar sin Tauri como bloqueante.
 */
class InMemoryAdapter implements CategoriasStorePersistence {
  private data: CategoriasStoreSchema | null = null;

  async load() {
    return this.data;
  }

  async save(data: CategoriasStoreSchema) {
    this.data = data;
  }
}

// ─── Factory ───────────────────────────────────────────────────────────────────

/** Singleton del adapter. Se crea una sola vez por sesión. */
let _adapter: CategoriasStorePersistence | null = null;

/**
 * Detecta el entorno y retorna el adapter correcto.
 *
 * - Si `window.__TAURI__` existe → TauriStoreAdapter (desktop)
 * - Si no → InMemoryAdapter (dev/browser)
 */
export function crearAdapter(): CategoriasStorePersistence {
  if (_adapter) return _adapter;

  if (typeof window !== 'undefined' && '__TAURI__' in window) {
    _adapter = new TauriStoreAdapter();
  } else {
    console.warn(
      '[CategoríasUI] Tauri no detectado. Usando almacenamiento en memoria (datos no se persisten).'
    );
    _adapter = new InMemoryAdapter();
  }

  return _adapter;
}
