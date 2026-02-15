import { useQuery } from '@tanstack/react-query';
import { promocionesApi } from '../api/promocionesApi';

export function usePromociones(estado = null) {
  return useQuery({
    queryKey: ['promociones', estado],
    queryFn: () => promocionesApi.listar(estado),
  });
}
