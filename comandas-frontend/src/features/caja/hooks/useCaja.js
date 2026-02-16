import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { cajaApi } from '../api/cajaApi';

export function useReporteCaja(fecha) {
  return useQuery({
    queryKey: ['reporte-caja', fecha],
    queryFn: () => cajaApi.obtenerReporte(fecha),
    enabled: !!fecha,
  });
}

/**
 * HU-13: Registrar egreso de caja.
 * Invalida el prefijo ['reporte-caja'] para refrescar balance de efectivo.
 */
export function useRegistrarEgreso() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: cajaApi.registrarEgreso,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reporte-caja'], exact: false });
    },
    onError: (error) => {
      console.error('[useRegistrarEgreso] Error al registrar egreso:', error);
    },
  });
}
