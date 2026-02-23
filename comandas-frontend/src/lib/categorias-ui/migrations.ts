import type { CategoriasStoreSchema } from './types';
import { SCHEMA_VERSION } from './types';
import { CATEGORIAS_DEFAULT } from './defaults';

/**
 * Migraciones del esquema.
 *
 * Cada función transforma de la versión N a la N+1.
 * Se ejecutan en cadena hasta llegar a SCHEMA_VERSION.
 */
const MIGRACIONES: Record<number, (data: unknown) => CategoriasStoreSchema> = {
  // v0 (no existe / primera vez) → v1
  0: () => crearDesdeDefaults(),

  // v1 → v2 (ejemplo futuro — descomentar cuando se necesite)
  // 1: (data) => ({
  //   ...(data as CategoriasStoreSchema),
  //   schemaVersion: 2,
  //   categorias: (data as CategoriasStoreSchema).categorias.map(c => ({
  //     ...c,
  //     icono: null,
  //   })),
  // }),
};

/**
 * Genera un esquema limpio desde los defaults.
 */
export function crearDesdeDefaults(): CategoriasStoreSchema {
  return {
    schemaVersion: SCHEMA_VERSION,
    categorias: [...CATEGORIAS_DEFAULT],
    ordenProductos: {},
  };
}

/**
 * Migra un esquema viejo (o null) a la versión actual.
 *
 * Ejecuta las funciones de migración en cadena:
 * v0 → v1 → v2 → ... → SCHEMA_VERSION
 *
 * Si no existe migración para una versión intermedia,
 * resetea a defaults como fallback seguro.
 */
export function migrateSchema(data: unknown): CategoriasStoreSchema {
  const raw = data as Record<string, unknown> | null | undefined;
  let current = (raw?.schemaVersion as number) ?? 0;
  let resultado: unknown = raw ?? {};

  while (current < SCHEMA_VERSION) {
    const migrar = MIGRACIONES[current];
    if (!migrar) {
      console.warn(
        `[CategoríasUI] Sin migración para v${current}. Reseteando a defaults.`
      );
      return crearDesdeDefaults();
    }
    resultado = migrar(resultado);
    current++;
  }

  return resultado as CategoriasStoreSchema;
}

/**
 * Valida y sanitiza un esquema cargado del store.
 *
 * Protege contra:
 * - JSON corrupto / estructura inválida
 * - Categorías con datos faltantes
 * - Versiones incompatibles
 */
export function validarYSanitizar(
  raw: unknown
): CategoriasStoreSchema {
  // Schema vacío o corrupto
  if (!raw || typeof raw !== 'object') {
    return crearDesdeDefaults();
  }

  const data = raw as Record<string, unknown>;

  // Versión incompatible → migrar
  if (data.schemaVersion !== SCHEMA_VERSION) {
    return migrateSchema(raw);
  }

  // Validar estructura mínima
  if (!Array.isArray(data.categorias)) {
    return crearDesdeDefaults();
  }

  // Sanitizar: eliminar categorías con datos inválidos
  const categorias = (data.categorias as Record<string, unknown>[]).filter(
    (c) =>
      typeof c.id === 'string' &&
      typeof c.nombre === 'string' &&
      typeof c.colorBase === 'string' &&
      typeof c.orden === 'number'
  );

  // Si quedaron vacías, resetear
  if (categorias.length === 0) {
    return crearDesdeDefaults();
  }

  return {
    schemaVersion: SCHEMA_VERSION,
    categorias: categorias as unknown as CategoriasStoreSchema['categorias'],
    ordenProductos:
      (data.ordenProductos as Record<string, string[]>) ?? {},
  };
}
