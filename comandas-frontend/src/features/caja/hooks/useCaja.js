import { useQuery } from '@tanstack/react-query';
import { cajaApi } from '../api/cajaApi';

export function useReporteCaja(fecha) {
  return useQuery({
    queryKey: ['reporte-caja', fecha],
    queryFn: () => cajaApi.obtenerReporte(fecha),
    enabled: !!fecha,
  });
}
