import { useQuery } from '@tanstack/react-query';
import { productosApi } from '../api/productosApi';

export function useProductos(color = null) {
  return useQuery({
    queryKey: ['productos', color],
    queryFn: () => productosApi.listar(color),
  });
}
