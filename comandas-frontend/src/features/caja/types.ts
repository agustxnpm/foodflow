/**
 * Tipos de dominio para el módulo Caja (Egresos + Arqueo/Reporte)
 *
 * Refleja los DTOs del backend: EgresoRequestBody, EgresoResponse,
 * ReporteCajaResponse.
 *
 * @see backend: com.agustinpalma.comandas.application.dto
 */

import type { MedioPago } from '../salon/types';

// ─── Enums ────────────────────────────────────────────────────────────────────

/**
 * Tipo de movimiento de caja.
 * Actualmente solo soporta EGRESO (salida de efectivo).
 */
export type TipoMovimiento = 'EGRESO';

// ─── Requests ─────────────────────────────────────────────────────────────────

/**
 * Body HTTP para registrar un egreso de caja.
 * Refleja EgresoRequestBody del backend.
 */
export interface EgresoRequest {
  /** Monto del egreso (debe ser > 0) */
  monto: number;
  /** Descripción del egreso (ej: "Productos de limpieza") */
  descripcion: string;
}

// ─── Responses ────────────────────────────────────────────────────────────────

/**
 * Respuesta al registrar un egreso de caja.
 * Refleja EgresoResponse del backend.
 */
export interface EgresoResponse {
  id: string;
  monto: number;
  descripcion: string;
  /** ISO 8601 datetime */
  fecha: string;
  /** Tipo de movimiento (EGRESO) */
  tipo: string;
  /** Número de comprobante generado automáticamente */
  numeroComprobante: string;
}

/**
 * Resumen de un movimiento de caja para el reporte (arqueo).
 * Refleja MovimientoResumen anidado en ReporteCajaResponse.
 */
export interface MovimientoResumen {
  id: string;
  monto: number;
  descripcion: string;
  /** ISO 8601 datetime */
  fecha: string;
  numeroComprobante: string;
}

/**
 * Reporte de caja diario (arqueo).
 * Refleja ReporteCajaResponse del backend.
 *
 * @param totalVentasReales suma de pedidos cerrados excluyendo pagos A_CUENTA
 * @param totalConsumoInterno suma de pagos A_CUENTA
 * @param totalEgresos suma de movimientos de caja
 * @param balanceEfectivo (total pagos EFECTIVO) − (total egresos)
 */
export interface ReporteCajaResponse {
  /** Suma de pedidos cerrados excluyendo pagos A_CUENTA */
  totalVentasReales: number;
  /** Suma de pagos A_CUENTA (consumo interno / empleados) */
  totalConsumoInterno: number;
  /** Suma de movimientos de caja (egresos) */
  totalEgresos: number;
  /** (total pagos EFECTIVO) − (total egresos) */
  balanceEfectivo: number;
  /** Mapa con el total por cada medio de pago comercial */
  desglosePorMedioPago: Record<MedioPago, number>;
  /** Lista de movimientos de caja del día */
  movimientos: MovimientoResumen[];
}
