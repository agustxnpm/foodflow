/**
 * Tipos para impresión (Comanda operativa + Ticket de venta)
 *
 * Refleja los DTOs del backend: ComandaImpresionResponse, TicketImpresionResponse.
 *
 * Comanda → solo datos operativos para cocina/barra (sin precios).
 * Ticket  → resumen financiero para el cliente (con desglose de descuentos).
 *
 * Diseñados para renderizado en impresora térmica desde el frontend.
 *
 * @see backend: com.agustinpalma.comandas.application.dto
 */

// ─── Comanda (cocina/barra) ───────────────────────────────────────────────────

/** Header de la comanda operativa */
export interface HeaderComanda {
  numeroMesa: number;
  numeroPedido: number;
  /** ISO 8601 datetime */
  fechaHora: string;
}

/**
 * Ítem de la comanda operativa.
 * Solo información operativa: qué preparar, cuánto y observaciones.
 * NO incluye valores monetarios — la cocina no necesita precios.
 */
export interface ItemComanda {
  cantidad: number;
  nombreProducto: string;
  observaciones?: string;
}

/**
 * Comanda operativa para cocina/barra (HU-05).
 * Solo lectura, inmutable. Sin información financiera.
 */
export interface ComandaImpresionResponse {
  header: HeaderComanda;
  items: ItemComanda[];
}

// ─── Ticket de venta (cliente) ────────────────────────────────────────────────

/** Header del ticket con datos del local */
export interface HeaderTicket {
  nombreLocal: string;
  direccion?: string;
  telefono?: string;
  cuit?: string;
  /** ISO 8601 datetime */
  fechaHora: string;
  numeroMesa: number;
}

/** Ítem del ticket con precios */
export interface ItemTicket {
  cantidad: number;
  descripcion: string;
  precioUnitario: number;
  importe: number;
}

/**
 * Desglose de un ajuste económico individual en el ticket.
 * Reemplaza DesglosePromocion con soporte para cualquier tipo de descuento.
 */
export interface DesgloseAjuste {
  tipo: 'PROMOCION' | 'MANUAL';
  descripcion: string;
  /** Monto del ajuste */
  monto: number;
}

/** Totales del ticket con desglose de descuentos */
export interface TotalesTicket {
  subtotal: number;
  montoDescuentoPromos: number;
  montoDescuentoManual: number;
  totalFinal: number;
  desgloseAjustes: DesgloseAjuste[];
}

/** Footer del ticket */
export interface FooterTicket {
  mensajeBienvenida?: string;
}

/**
 * Ticket de venta para el cliente (HU-29).
 * Contiene resumen financiero con totales, descuentos y datos del local.
 * Diseñado para renderizado en impresora térmica.
 */
export interface TicketImpresionResponse {
  header: HeaderTicket;
  items: ItemTicket[];
  totales: TotalesTicket;
  footer: FooterTicket;
}
