import { useQuery } from '@tanstack/react-query';
import apiClient from '../lib/apiClient';
import useIdentityStore from '../store/useIdentityStore';
import { useEffect } from 'react';

interface IdentityConfigured {
  configured: true;
  localId: string;
  nombreLocal: string;
  direccion: string;
  telefono: string;
  cuit: string;
  mensajeBienvenida: string;
}

interface IdentityNotConfigured {
  configured: false;
}

export type IdentityResponse = IdentityConfigured | IdentityNotConfigured;

async function fetchIdentity(): Promise<IdentityResponse> {
  const { data } = await apiClient.get<IdentityResponse>('/config/identity');
  return data;
}

/**
 * Hook que consulta el estado de activación del sistema.
 *
 * - Si el backend responde { configured: true }, guarda localId en el store global.
 * - Si responde { configured: false }, el store queda vacío (app no activada).
 * - En caso de error de red, falla silenciosamente (isError: true).
 *
 * Se usa en AppRouter para decidir si mostrar PantallaActivacion o el flujo normal.
 */
export function useIdentidad() {
  const setIdentity = useIdentityStore((s) => s.setIdentity);

  const query = useQuery({
    queryKey: ['identity'],
    queryFn: fetchIdentity,
    staleTime: Infinity,
    retry: 1,
  });

  useEffect(() => {
    if (query.data?.configured) {
      setIdentity(query.data.localId, query.data.nombreLocal);
    }
  }, [query.data, setIdentity]);

  return {
    data: query.data ?? null,
    isLoading: query.isLoading,
    isError: query.isError,
  };
}
