import { create } from 'zustand';

interface IdentityState {
  localId: string | null;
  nombreLocal: string | null;
  configured: boolean;
  setIdentity: (localId: string, nombreLocal: string) => void;
  clear: () => void;
}

/**
 * Store global de identidad del tenant.
 *
 * Se carga una sola vez al arrancar la app (desde useIdentidad).
 * Lo consume el interceptor de apiClient para inyectar X-Local-Id,
 * y useLicencia para validar la licencia contra el Gist.
 */
const useIdentityStore = create<IdentityState>((set) => ({
  localId: null,
  nombreLocal: null,
  configured: false,
  setIdentity: (localId, nombreLocal) =>
    set({ localId, nombreLocal, configured: true }),
  clear: () =>
    set({ localId: null, nombreLocal: null, configured: false }),
}));

export default useIdentityStore;
