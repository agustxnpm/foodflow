import type { CategoriaUI } from './types';

/**
 * Categorías por defecto — espejo de las hardcodeadas en `lib/categorias.ts`.
 *
 * Se usan para:
 * 1. Primera inicialización del store (día 0 tras update)
 * 2. Fallback si el store está corrupto
 *
 * Los IDs son determinísticos (`default-*`) para que la migración
 * sea idempotente: si se ejecuta dos veces, no se duplican.
 */
export const CATEGORIAS_DEFAULT: CategoriaUI[] = [
  {
    id: 'default-hamburguesas',
    nombre: 'Hamburguesas',
    colorBase: '#FF0000',
    colorDisplay: '#FF0000',
    esExtra: false,
    orden: 0,
  },
  {
    id: 'default-bebidas',
    nombre: 'Bebidas',
    colorBase: '#0000FF',
    colorDisplay: '#3B82F6',
    esExtra: false,
    orden: 1,
  },
  {
    id: 'default-ensaladas',
    nombre: 'Ensaladas',
    colorBase: '#00FF00',
    colorDisplay: '#22C55E',
    esExtra: false,
    orden: 2,
  },
  {
    id: 'default-postres',
    nombre: 'Postres',
    colorBase: '#FFA500',
    colorDisplay: '#F97316',
    esExtra: false,
    orden: 3,
  },
  {
    id: 'default-pizzas',
    nombre: 'Pizzas',
    colorBase: '#800080',
    colorDisplay: '#A855F7',
    esExtra: false,
    orden: 4,
  },
  {
    id: 'default-extras',
    nombre: 'Extras',
    colorBase: '#FFFF00',
    colorDisplay: '#EAB308',
    esExtra: true,
    orden: 5,
  },
];
