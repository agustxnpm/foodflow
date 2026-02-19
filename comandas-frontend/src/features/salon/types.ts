/**
 * Tipos de dominio para el módulo Salón (Mesas + Cierre)
 *
 * Refleja los DTOs del backend: MesaResponse, AbrirMesaRequest/Response,
 * CerrarMesaRequest/Response, ReabrirPedidoResponse, CrearMesaRequest.
 *
 * @see backend: com.agustinpalma.comandas.application.dto
 */

// ─── Enums compartidos ────────────────────────────────────────────────────────

/** Estado de una mesa en el salón */
export type EstadoMesa = 'LIBRE' | 'ABIERTA';

/** Estado del ciclo de vida de un pedido */
export type EstadoPedido = 'ABIERTO' | 'CERRADO';

/**
 * Medio de pago aceptado por el local.
 * A_CUENTA = consumo interno (empleados), no cuenta como venta real.
 */
export type MedioPago = 'EFECTIVO' | 'TARJETA' | 'TRANSFERENCIA' | 'QR' | 'A_CUENTA';

// ─── Entidades ────────────────────────────────────────────────────────────────

/**
 * Entidad Mesa del dominio.
 * Refleja MesaResponse del backend (id = UUID string).
 */
export interface Mesa {
  id: string;
  numero: number;
  estado: EstadoMesa;
}

// ─── Requests ─────────────────────────────────────────────────────────────────

/** DTO para crear una nueva mesa */
export interface CrearMesaRequest {
  numero: number;
}

/** DTO de entrada para abrir una mesa (iniciar pedido) */
export interface AbrirMesaRequest {
  mesaId: string;
}

/** Pago individual dentro del cierre de mesa */
export interface PagoRequest {
  /** Medio de pago utilizado */
  medio: MedioPago;
  /** Monto del pago (debe ser > 0) */
  monto: number;
}

/** Body HTTP para cerrar una mesa con pagos split (HU-04, HU-12). El mesaId viaja como path param. */
export interface CerrarMesaRequest {
  pagos: PagoRequest[];
}

// ─── Responses ────────────────────────────────────────────────────────────────

/** Respuesta al abrir una mesa: mesa + pedido creado */
export interface AbrirMesaResponse {
  mesaId: string;
  numeroMesa: number;
  estadoMesa: string;
  pedidoId: string;
  numeroPedido: number;
  estadoPedido: string;
  /** ISO 8601 datetime */
  fechaApertura: string;
}

/** Pago registrado en la respuesta de cierre */
export interface PagoResponse {
  medio: MedioPago;
  monto: number;
  /** ISO 8601 datetime */
  fecha: string;
}

/** Respuesta al cerrar una mesa: snapshot contable congelado */
export interface CerrarMesaResponse {
  mesaId: string;
  mesaNumero: number;
  mesaEstado: EstadoMesa;
  pedidoId: string;
  pedidoEstado: EstadoPedido;
  /** Subtotal congelado (antes de descuentos) */
  montoSubtotal: number;
  /** Monto total de descuentos aplicados */
  montoDescuentos: number;
  /** Total final congelado */
  montoTotal: number;
  pagos: PagoResponse[];
  /** ISO 8601 datetime */
  fechaCierre: string;
}

/** Respuesta a la reapertura de un pedido cerrado (HU-14) */
export interface ReabrirPedidoResponse {
  mesaId: string;
  mesaNumero: number;
  mesaEstado: EstadoMesa;
  pedidoId: string;
  pedidoNumero: number;
  pedidoEstado: EstadoPedido;
  /** ISO 8601 datetime */
  fechaReapertura: string;
  /** Cantidad de ítems que conserva el pedido reabierto */
  cantidadItems: number;
}


