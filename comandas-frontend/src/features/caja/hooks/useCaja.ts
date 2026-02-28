/**
 * Hooks de TanStack Query v5 para el módulo Caja (Control Financiero Diario).
 *
 * Estrategia de invalidación: jerárquica por prefijo (`exact: false`).
 * Cada mutación invalida las query keys mínimas necesarias para que
 * la UI refleje el estado real del backend sin refetches innecesarios.
 *
 * Hooks expuestos:
 *   useReporteCaja       → Query del arqueo diario + datos derivados
 *   useRegistrarEgreso   → Mutation de egreso + stub ESC/POS
 *   useCerrarJornada     → Mutation de cierre + manejo HTTP 400
 *   useReabrirPedido     → Mutation con invalidación cruzada (3 dominios)
 *
 * @see cajaApi.ts — capa de transporte HTTP
 * @see types.ts   — contratos de dominio (DTOs, errores)
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { isAxiosError } from 'axios';

import { cajaApi } from '../api/cajaApi';
import type {
  EgresoRequest,
  EgresoResponse,
  ReporteCajaResponse,
  ReporteCajaDerivado,
  CierreJornadaErrorData,
  DetallePedidoCerrado,
  CorreccionPedidoRequest,
  JornadaResumen,
} from '../types';
import { MesasAbiertasError, JornadaYaCerradaError } from '../types';

// ─── Query Keys (centralizadas para consistencia) ─────────────────────────────

export const cajaKeys = {
  /** Prefijo raíz: invalida todo lo relacionado a caja */
  all: ['reporte-caja'] as const,
  /** Reporte filtrado por fecha */
  reporte: (fecha: string) => ['reporte-caja', fecha] as const,
  /** Detalle de pedido cerrado para corrección */
  detallePedido: (pedidoId: string) => ['reporte-caja', 'pedido', pedidoId] as const,
  /** Historial de jornadas por rango */
  historialJornadas: (desde: string, hasta: string) => ['jornadas-caja', desde, hasta] as const,
} as const;

// ─── Stub ESC/POS ─────────────────────────────────────────────────────────────

/**
 * Stub de impresión ESC/POS para ticket de egreso.
 *
 * En el futuro, se comunicará con Tauri vía:
 *   window.__TAURI__.invoke('imprimir_ticket', { buffer })
 *
 * Por ahora solo genera un log estructurado del buffer de impresión.
 *
 * @param egreso - Datos del egreso registrado (respuesta del backend)
 */
function imprimirTicketEgreso(egreso: EgresoResponse): void {
  const buffer = [
    '================================',
    '        EGRESO DE CAJA          ',
    '================================',
    `Comprobante: ${egreso.numeroComprobante}`,
    `Fecha:       ${egreso.fecha}`,
    `Monto:       $${egreso.monto.toFixed(2)}`,
    `Motivo:      ${egreso.descripcion}`,
    '================================',
    '',
  ].join('\n');

  // TODO: Reemplazar por invoke Tauri cuando el módulo de impresión esté listo
  // await window.__TAURI__.invoke('imprimir_ticket_egreso', { buffer });
  console.log('[ESC/POS Stub] imprimirTicketEgreso →\n', buffer);
}

// ─── useReporteCaja ───────────────────────────────────────────────────────────

/**
 * HU-13: Consulta del reporte de caja diario (arqueo).
 *
 * Trae la "foto" del día desde el backend y calcula datos derivados
 * mediante `select` (transformación in-memory, sin re-fetch):
 *
 * - **balanceFisicoEsperado**: Fondo Inicial + Ventas EFECTIVO − Egresos.
 *   Representa el efectivo físico que debería haber en caja.
 *
 * - **ingresoNeto**: Total de ventas comerciales reales (excluye A_CUENTA).
 *   Alias semántico de `totalVentasReales` para la UI de cierre.
 *
 * @param fecha        - Fecha ISO (YYYY-MM-DD). Si es falsy, la query no se ejecuta.
 * @param fondoInicial - Efectivo con el que se abrió la jornada (default 0).
 *                       ⚠️ Concepto no modelado aún en backend.
 *
 * @example
 * const { data, isLoading } = useReporteCaja('2026-02-27', 5000);
 * // data.balanceFisicoEsperado → 5000 + EFECTIVO − egresos
 * // data.ingresoNeto           → ventas sin A_CUENTA
 * // data.desglosePorMedioPago  → { EFECTIVO: ..., TARJETA: ..., QR: ... }
 */
export function useReporteCaja(fecha: string, fondoInicial = 0) {
  return useQuery<ReporteCajaResponse, Error, ReporteCajaDerivado>({
    queryKey: cajaKeys.reporte(fecha),
    queryFn: () => cajaApi.obtenerReporte(fecha),
    enabled: !!fecha,
    refetchInterval: 30_000, // Polling cada 30s — mantiene UI sincronizada sin F5 (Tauri)
    select: (data): ReporteCajaDerivado => ({
      ...data,
      // balanceEfectivo del backend = EFECTIVO − totalEgresos
      // Le sumamos el fondo inicial para obtener el arqueo completo
      balanceFisicoEsperado: fondoInicial + data.balanceEfectivo,
      // totalVentasReales ya excluye A_CUENTA por definición del backend
      ingresoNeto: data.totalVentasReales,
    }),
  });
}

// ─── useRegistrarEgreso ───────────────────────────────────────────────────────

/**
 * HU-13: Registrar egreso de caja (salida de efectivo).
 *
 * Flujo onSuccess:
 *   1. Invalida `['reporte-caja']` (exact: false) → refresca balance en tiempo real
 *   2. Llama al stub ESC/POS para preparar impresión del comprobante
 *
 * La invalidación por prefijo garantiza que cualquier instancia de
 * `useReporteCaja` (sin importar la fecha pasada) se refresque.
 *
 * @example
 * const { mutate } = useRegistrarEgreso();
 * mutate({ monto: 500, descripcion: 'Productos de limpieza' });
 */
export function useRegistrarEgreso() {
  const queryClient = useQueryClient();

  return useMutation<EgresoResponse, Error, EgresoRequest>({
    mutationFn: (data) => cajaApi.registrarEgreso(data),
    onSuccess: (egreso) => {
      // 1. Refrescar reporte de caja para actualizar balance
      queryClient.invalidateQueries({
        queryKey: cajaKeys.all,
        exact: false,
      });

      // 2. Preparar buffer ESC/POS (stub → Tauri en el futuro)
      imprimirTicketEgreso(egreso);
    },
    onError: (error) => {
      console.error('[useRegistrarEgreso] Error al registrar egreso:', error);
    },
  });
}

// ─── useCerrarJornada ─────────────────────────────────────────────────────────

/**
 * Cierre de jornada diaria.
 *
 * Maneja los 3 escenarios del backend:
 *
 * - **HTTP 200**: Jornada cerrada → invalida reporte y mesas.
 * - **HTTP 400** (mesas abiertas): Transforma en `MesasAbiertasError`
 *   para que la UI muestre alerta crítica con conteo de mesas.
 * - **HTTP 409** (jornada ya cerrada): Transforma en `JornadaYaCerradaError`
 *   para que la UI deshabilite el botón y muestre mensaje informativo.
 * - **Otros errores**: Se propagan tal cual para el sistema de Toasts genérico.
 *
 * La transformación del error ocurre en `mutationFn` (no en `onError`)
 * para que `mutation.error` ya contenga el tipo correcto.
 *
 * @example
 * const cierre = useCerrarJornada();
 * cierre.mutate(undefined, {
 *   onError: (error) => {
 *     if (error instanceof MesasAbiertasError) { ... }
 *     if (error instanceof JornadaYaCerradaError) { ... }
 *   },
 * });
 */
export function useCerrarJornada() {
  const queryClient = useQueryClient();

  return useMutation<void, Error | MesasAbiertasError | JornadaYaCerradaError>({
    mutationFn: async () => {
      try {
        await cajaApi.cerrarJornada();
      } catch (error: unknown) {
        if (isAxiosError(error)) {
          // HTTP 400 → mesas abiertas
          if (error.response?.status === 400) {
            const data = error.response.data as CierreJornadaErrorData;
            throw new MesasAbiertasError(
              data.mensaje ??
                'Existen mesas abiertas. Cierre todas las mesas antes de cerrar la jornada.',
              data.mesasAbiertas,
            );
          }
          // HTTP 409 → jornada ya cerrada para la fecha operativa
          if (error.response?.status === 409) {
            const data = error.response.data as { message?: string };
            throw new JornadaYaCerradaError(
              data.message ?? 'La jornada de hoy ya fue cerrada.',
            );
          }
        }
        // Cualquier otro error se propaga sin transformar
        throw error;
      }
    },
    onSuccess: () => {
      // Refrescar dominios afectados por el cierre
      queryClient.invalidateQueries({ queryKey: cajaKeys.all, exact: false });
      queryClient.invalidateQueries({ queryKey: ['jornadas-caja'], exact: false });
      queryClient.invalidateQueries({ queryKey: ['mesas'], exact: false });
    },
    onError: (error) => {
      // Log para debugging; la UI maneja la presentación
      console.error('[useCerrarJornada] Error:', error.message);
    },
  });
}

// ─── useDetallePedidoCerrado ──────────────────────────────────────────────────

/**
 * Consulta el detalle completo de un pedido cerrado (ítems + pagos).
 *
 * Usado por el modal de corrección para cargar los datos editables.
 * Solo se ejecuta cuando `pedidoId` es truthy (enabled condicional).
 *
 * @param pedidoId - UUID del pedido a consultar (null/undefined = deshabilitado)
 */
export function useDetallePedidoCerrado(pedidoId: string | null) {
  return useQuery<DetallePedidoCerrado, Error>({
    queryKey: cajaKeys.detallePedido(pedidoId ?? ''),
    queryFn: () => cajaApi.obtenerDetallePedido(pedidoId!),
    enabled: !!pedidoId,
  });
}

// ─── useCorregirPedido ────────────────────────────────────────────────────────

/**
 * Corregir un pedido cerrado sin reabrir la mesa.
 *
 * Alternativa segura a la reapertura: modifica cantidades de ítems
 * y pagos directamente sobre el pedido CERRADO.
 *
 * La mesa NO se modifica — el salón no se ve afectado.
 *
 * Invalidaciones:
 *   1. `['reporte-caja']` → Refrescar reporte/arqueo con los nuevos montos
 *
 * No invalida `['mesas']` ni `['pedido']` porque la corrección no afecta
 * el estado de la mesa ni el flujo del salón.
 */
export function useCorregirPedido() {
  const queryClient = useQueryClient();

  return useMutation<
    DetallePedidoCerrado,
    Error,
    { pedidoId: string; data: CorreccionPedidoRequest }
  >({
    mutationFn: ({ pedidoId, data }) => cajaApi.corregirPedido(pedidoId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: cajaKeys.all, exact: false });
    },
    onError: (error) => {
      console.error('[useCorregirPedido] Error al corregir pedido:', error);
    },
  });
}

// ─── useHistorialJornadas ─────────────────────────────────────────────────────

/**
 * Consulta el historial de jornadas cerradas dentro de un rango de fechas.
 *
 * Usado por la pantalla de consulta histórica para:
 *   - Lista expandible de cierres de caja
 *   - Gráfico interactivo de evolución financiera
 *
 * La query se ejecuta solo cuando ambas fechas son truthy.
 * El staleTime se aumenta a 5 min porque los datos históricos no cambian.
 *
 * @param desde - Fecha operativa inicial (YYYY-MM-DD)
 * @param hasta - Fecha operativa final (YYYY-MM-DD)
 */
export function useHistorialJornadas(desde: string, hasta: string) {
  return useQuery<JornadaResumen[], Error>({
    queryKey: cajaKeys.historialJornadas(desde, hasta),
    queryFn: () => cajaApi.obtenerHistorialJornadas(desde, hasta),
    enabled: !!desde && !!hasta,
    staleTime: 5 * 60 * 1000, // 5 min — datos históricos inmutables
  });
}
