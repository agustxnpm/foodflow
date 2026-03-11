import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { configApi } from '../app/configApi';

/**
 * Lista las impresoras instaladas en el SO donde corre el backend.
 * El polling no es necesario — las impresoras se detectan al abrir Ajustes.
 */
export function useImpresoras() {
  return useQuery<string[]>({
    queryKey: ['config', 'impresoras'],
    queryFn: async () => {
      const { data } = await configApi.listarImpresoras();
      return data;
    },
  });
}

/**
 * Obtiene la impresora predeterminada guardada para el local actual.
 */
export function useImpresoraPredeterminada() {
  return useQuery<string>({
    queryKey: ['config', 'impresora-predeterminada'],
    queryFn: async () => {
      const { data } = await configApi.obtenerImpresoraPredeterminada();
      return data.impresora;
    },
  });
}

/**
 * Guarda la impresora seleccionada como predeterminada.
 * Invalida la query de impresora predeterminada al guardar.
 */
export function useGuardarImpresoraPredeterminada() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (nombre: string) => configApi.guardarImpresoraPredeterminada(nombre),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['config', 'impresora-predeterminada'] });
    },
  });
}
