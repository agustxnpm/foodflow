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

/** Extra agrupado para mostrar en el ticket (ej: 2x Disco de carne) */
export interface ExtraTicket {
  nombre: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

/** Ítem del ticket con precios y extras */
export interface ItemTicket {
  cantidad: number;
  descripcion: string;
  precioUnitario: number;
  importe: number;
  /** Extras agrupados asociados a este ítem (puede estar vacío) */
  extras: ExtraTicket[];
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

// ─── Envío a cocina (HU-29) ──────────────────────────────────────────────────

/**
 * Respuesta del backend al enviar comanda a cocina.
 * Contiene los bytes ESC/POS codificados en Base64 listos para imprimir.
 *
 * @see backend: EnviarComandaResponse
 */
export interface EnviarComandaResponse {
  /** Buffer ESC/POS codificado en Base64 */
  escPosBase64: string;
  /** Timestamp del envío (ISO 8601) */
  timestampEnvio: string;
  /** Cantidad de ítems marcados como nuevos en esta comanda */
  cantidadItemsNuevos: number;
  /** Cantidad total de ítems en el pedido */
  cantidadItemsTotal: number;
}

// ─── Ticket de venta ESC/POS (HU-29) ─────────────────────────────────────────

/**
 * Respuesta del backend al generar un ticket de venta ESC/POS.
 * Solo contiene los bytes ESC/POS codificados en Base64.
 *
 * @see backend: TicketVentaEscPosResponse
 */
export interface TicketVentaEscPosResponse {
  /** Buffer ESC/POS codificado en Base64 */
  escPosBase64: string;
}
