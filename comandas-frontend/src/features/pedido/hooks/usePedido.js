import { useMutation, useQueryClient } from '@tanstack/react-query';
import { pedidosApi } from '../api/pedidosApi';

export function useAgregarProducto() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ pedidoId, ...data }) => pedidosApi.agregarProducto(pedidoId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pedido'] });
    },
  });
}
