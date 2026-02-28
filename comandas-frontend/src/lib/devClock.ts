/**
 * Estado global de fecha para desarrollo (time-travel).
 *
 * Cuando el panel DevTimeTravelPanel fija una hora en el backend,
 * también setea esta fecha para que los componentes del frontend
 * (como CajaPage) pidan datos de la fecha correcta.
 *
 * En producción, este módulo exporta null y los componentes
 * usan new Date() normalmente.
 *
 * ⚠️ NO usar React state ni Zustand — esto es un singleton mutable
 * que se lee sincrónicamente desde funciones utilitarias.
 */

/** Fecha ISO override del dev clock, o null si tiempo real */
let devDateOverride: string | null = null;

/** Listeners que se notifican cuando cambia la fecha (para React re-renders) */
const listeners = new Set<() => void>();

export function getDevDateOverride(): string | null {
  return devDateOverride;
}

export function setDevDateOverride(fecha: string | null): void {
  devDateOverride = fecha;
  listeners.forEach((fn) => fn());
}

/** Suscribirse a cambios (para useSyncExternalStore si fuera necesario) */
export function subscribeDevDate(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}
