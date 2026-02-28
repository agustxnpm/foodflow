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
  /**
   * Tipo de movimiento: 'EGRESO' | 'PEDIDO'.
   *
   * Actualmente el backend solo devuelve egresos en la lista.
   * Cuando se extienda a incluir ventas/pedidos cerrados, este campo
   * permitirá diferenciar filas y habilitar acciones como reapertura.
   *
   * Si es undefined se asume EGRESO (compatibilidad backward).
   */
  tipo?: 'EGRESO' | 'PEDIDO';
}

/**
 * Resumen de un pedido cerrado (venta) para el historial de caja.
 * Refleja VentaResumen anidado en ReporteCajaResponse del backend.
 *
 * Contiene la información operativa necesaria para la UI:
 * identificación, mesa, monto total y fecha de cierre.
 */
export interface VentaResumen {
  /** UUID del pedido (para operaciones como reapertura) */
  pedidoId: string;
  /** Número secuencial visible del pedido */
  numeroPedido: number;
  /** Número de la mesa donde se consumíó */
  mesaNumero: number;
  /** Monto total cobrado (snapshot contable) */
  total: number;
  /** ISO 8601 datetime — momento en que se cerró el pedido */
  fechaCierre: string;
}

/**
 * Detalle de un pago individual vinculado a su pedido y mesa.
 * Refleja PagoDetalle anidado en ReporteCajaResponse del backend.
 *
 * Permite agrupar por medio de pago en la UI y navegar al pedido asociado.
 */
export interface PagoDetalle {
  /** UUID del pedido al que pertenece este pago */
  pedidoId: string;
  /** Número secuencial visible del pedido */
  numeroPedido: number;
  /** Número de la mesa donde se consumió */
  mesaNumero: number;
  /** Medio de pago utilizado */
  medioPago: MedioPago;
  /** Monto individual de este pago */
  monto: number;
  /** ISO 8601 datetime — momento en que se registró el pago */
  fecha: string;
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
  /** Lista de movimientos de caja del día (egresos) */
  movimientos: MovimientoResumen[];
  /** Lista de pedidos cerrados del día (historial de ventas) */
  ventas: VentaResumen[];
  /** Lista plana de pagos individuales con contexto pedido/mesa */
  pagosDetalle: PagoDetalle[];
  /** Indica si la jornada ya fue cerrada para la fecha consultada */
  jornadaCerrada: boolean;
}

// ─── Detalle de Pedido Cerrado (Corrección) ──────────────────────────────────

/**
 * Detalle completo de un pedido cerrado, incluyendo ítems y pagos.
 * Usado por el modal de corrección para mostrar y editar datos.
 * Refleja DetallePedidoCerradoResponse del backend.
 */
export interface DetallePedidoCerrado {
  pedidoId: string;
  numeroPedido: number;
  mesaNumero: number;
  /** ISO 8601 datetime */
  fechaCierre: string;
  items: ItemDetallePedido[];
  pagos: PagoDetallePedido[];
  montoSubtotal: number;
  montoDescuentos: number;
  montoTotal: number;
}

/** Detalle de un ítem del pedido (snapshot histórico). */
export interface ItemDetallePedido {
  itemId: string;
  nombreProducto: string;
  cantidad: number;
  precioUnitario: number;
  /** Subtotal bruto de la línea (precio × cantidad + extras, SIN descuentos) */
  subtotalLinea: number;
  observacion: string | null;
  /** Monto total de descuentos aplicados a esta línea (promo + manual) */
  montoDescuento: number;
  /** Descripción legible del descuento (ej: "2x1 Pizza", "Desc. manual: razón") */
  descripcionDescuento: string | null;
}

/** Detalle de un pago del pedido. */
export interface PagoDetallePedido {
  medio: MedioPago;
  monto: number;
}

/** Request para corregir un pedido cerrado. */
export interface CorreccionPedidoRequest {
  items: { itemId: string; cantidad: number }[];
  pagos: { medio: MedioPago; monto: number }[];
}

// ─── Historial de Jornadas ───────────────────────────────────────────────────

/**
 * Resumen de una jornada cerrada para la consulta histórica.
 * Refleja JornadaResumenResponse del backend.
 *
 * Cada registro es un snapshot contable de un día operativo cerrado.
 */
export interface JornadaResumen {
  /** UUID de la jornada */
  id: string;
  /** Fecha del día operativo (YYYY-MM-DD) — puede diferir del calendario si es turno noche */
  fechaOperativa: string;
  /** ISO 8601 datetime — momento exacto del cierre */
  fechaCierre: string;
  /** Suma de cobros reales (excluye A_CUENTA) */
  totalVentasReales: number;
  /** Suma de pagos A_CUENTA (consumo interno) */
  totalConsumoInterno: number;
  /** Suma de salidas de efectivo del día */
  totalEgresos: number;
  /** Entradas EFECTIVO − Egresos */
  balanceEfectivo: number;
  /** Cantidad de pedidos cerrados en la jornada */
  pedidosCerradosCount: number;
}

// ─── Reporte Derivado (cálculos frontend) ─────────────────────────────────────

/**
 * Reporte de caja enriquecido con datos derivados en frontend.
 * Extiende la respuesta cruda del backend con cálculos de arqueación.
 *
 * Estos valores NO se persisten ni se envían al backend;
 * son proyecciones de solo lectura para la UI de Caja.
 */
export interface ReporteCajaDerivado extends ReporteCajaResponse {
  /**
   * Arqueo esperado de efectivo físico en caja.
   *
   * Fórmula: Fondo Inicial + Ventas EFECTIVO − Egresos Manuales.
   *
   * ⚠️ `fondoInicial` no está modelado aún en backend.
   * Se recibe como parámetro del hook (default 0).
   */
  balanceFisicoEsperado: number;
  /**
   * Ingreso neto del día: ventas comerciales reales excluyendo A_CUENTA.
   * Semánticamente equivalente a `totalVentasReales` del backend,
   * pero con nombre explícito para la UI de cierre.
   */
  ingresoNeto: number;
}

// ─── Errores de dominio (Caja) ────────────────────────────────────────────────

/**
 * Estructura del body HTTP 400 al intentar cerrar jornada
 * cuando existen mesas con estado ABIERTO.
 *
 * Permite a la UI discriminar este caso y mostrar alerta crítica.
 */
export interface CierreJornadaErrorData {
  mensaje: string;
  mesasAbiertas?: number;
}

/**
 * Error de dominio: intento de cerrar jornada con mesas abiertas.
 *
 * La UI puede usar `instanceof MesasAbiertasError` para
 * renderizar una alerta específica diferenciada de errores genéricos.
 */
export class MesasAbiertasError extends Error {
  public readonly mesasAbiertas?: number;

  constructor(mensaje: string, mesasAbiertas?: number) {
    super(mensaje);
    this.name = 'MesasAbiertasError';
    this.mesasAbiertas = mesasAbiertas;
  }
}

/**
 * Error de dominio: la jornada ya fue cerrada para la fecha operativa.
 *
 * Ocurre al intentar cerrar dos veces el mismo día.
 * La UI debería mostrar un mensaje informativo (no crítico)
 * y deshabilitar el botón de cierre.
 */
export class JornadaYaCerradaError extends Error {
  constructor(mensaje: string) {
    super(mensaje);
    this.name = 'JornadaYaCerradaError';
  }
}
