import { useQuery } from '@tanstack/react-query';
import { mesasApi } from '../api/mesasApi';

export function useMesas() {
  return useQuery({
    queryKey: ['mesas'],
    queryFn: () => mesasApi.listar(),
  });
}
