import { QueryClient } from '@tanstack/react-query';

// Configuración de TanStack Query para FoodFlow
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Desactivar refetch automático en focus (app desktop)
      refetchOnWindowFocus: false,
      refetchOnMount: true,
      refetchOnReconnect: true,
      // Cache de 5 minutos
      staleTime: 1000 * 60 * 5,
      // Retry solo una vez
      retry: 1,
      // Evitar re-renders innecesarios
      structuralSharing: true,
    },
    mutations: {
      retry: false,
    },
  },
});
