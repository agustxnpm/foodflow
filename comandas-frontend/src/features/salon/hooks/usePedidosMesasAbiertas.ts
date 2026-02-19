import { useQueries } from '@tanstack/react-query';
import { mesasApi } from '../api/mesasApi';
import type { Mesa } from '../types';
import type { DetallePedidoResponse } from '../../pedido/types';

/**
 * Resumen de una mesa abierta para el sidebar.
 * Combina datos de Mesa + DetallePedidoResponse en una sola estructura
 * optimizada para renderizado rápido.
 */
export interface ResumenMesaAbierta {
  mesaId: string;
  numeroMesa: number;
  pedidoId: string;
  numeroPedido: number;
  /** ISO 8601 datetime — hora de apertura del pedido */
  fechaApertura: string;
  /** Total acumulado que paga el cliente (subtotal − descuentos) */
  totalAcumulado: number;
  cantidadItems: number;
}

/**
 * Hook que consulta en paralelo el pedido activo de cada mesa abierta.
 *
 * Usa useQueries para hacer N requests paralelos (uno por mesa ABIERTA),
 * reutilizando la cache de React Query con la misma queryKey ['pedido', mesaId]
 * que usa usePedidoMesa(). Esto significa que si el operador ya abrió el detalle
 * de una mesa, el sidebar muestra ese dato sin request adicional.
 *
 * @param mesasAbiertas - Lista de mesas con estado ABIERTA
 * @returns resumenes listos para renderizar + estado de carga global
 */
export function usePedidosMesasAbiertas(mesasAbiertas: Mesa[]) {
  const queries = useQueries({
    queries: mesasAbiertas.map((mesa) => ({
      queryKey: ['pedido', mesa.id],
      queryFn: () => mesasApi.consultarPedido(mesa.id),
      // Refresco suave cada 30s para mantener totales actualizados
      // sin saturar el backend en un local con pocas mesas
      staleTime: 30_000,
    })),
  });

  const cargando = queries.some((q) => q.isLoading);

  const resumenes: ResumenMesaAbierta[] = mesasAbiertas
    .map((mesa, i) => {
      const pedido = queries[i]?.data as DetallePedidoResponse | undefined;
      if (!pedido) return null;

      return {
        mesaId: mesa.id,
        numeroMesa: mesa.numero,
        pedidoId: pedido.pedidoId,
        numeroPedido: pedido.numeroPedido,
        fechaApertura: pedido.fechaApertura,
        totalAcumulado: pedido.totalParcial,
        cantidadItems: pedido.items.length,
      };
    })
    .filter((r): r is ResumenMesaAbierta => r !== null);

  const totalGeneral = resumenes.reduce((acc, r) => acc + r.totalAcumulado, 0);

  return {
    resumenes,
    totalGeneral,
    cargando,
  } as const;
}
