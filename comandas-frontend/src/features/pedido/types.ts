/**
 * Tipos de dominio para el módulo Pedido (Comanda Digital)
 *
 * Refleja los DTOs del backend: DetallePedidoResponse, AgregarProductoRequest/Response,
 * ModificarCantidadItemRequest, EliminarItemPedidoRequest, AplicarDescuentoManualRequest/Response.
 *
 * Regla de oro: El total del pedido se calcula a partir de los ítems base + descuentos
 * acumulables, nunca al revés. Los ítems son la verdad, los descuentos son capas.
 *
 * @see backend: com.agustinpalma.comandas.application.dto
 */

import type { EstadoPedido } from '../salon/types';

// ─── Items del pedido ─────────────────────────────────────────────────────────

/**
 * Ítem de un pedido (snapshot inmutable).
 * Refleja ItemPedidoDTO anidado en AgregarProductoResponse.
 *
 * Los descuentos están embebidos en cada ítem, no como entidad separada.
 * precioFinal = subtotalItem - descuentoTotal
 *
 * HU-10: Incluye info de promo para UX (precio tachado, ahorro).
 * HU-14: descuentoTotal incluye promo automática + descuento manual por ítem.
 */
export interface ItemPedido {
  itemId: string;
  productoId: string;
  nombreProducto: string;
  cantidad: number;
  /** Precio de lista — para tachar en UI cuando hay promo */
  precioUnitarioBase: number;
  /** precioBase × cantidad (sin descuento) */
  subtotalItem: number;
  /** Ahorro total del ítem (promo + manual) */
  descuentoTotal: number;
  /** Lo que paga el cliente (subtotal - descuento) */
  precioFinal: number;
  observacion?: string;
  /** Etiqueta de la promo aplicada (ej: "2x1 en Café") */
  nombrePromocion?: string;
  /** Flag para condicionales de UI */
  tienePromocion: boolean;
}

/**
 * Ítem con desglose detallado para la vista de detalle (HU-06).
 * Refleja ItemDetalleDTO del backend.
 */
export interface ItemDetalle {
  id: string;
  nombreProducto: string;
  cantidad: number;
  /** Precio de lista (para tachar en UI) */
  precioUnitarioBase: number;
  /** precioBase × cantidad */
  subtotal: number;
  /** Monto de descuento aplicado */
  descuentoTotal: number;
  /** Lo que paga el cliente */
  precioFinal: number;
  observacion?: string;
  /** Nombre de la promo aplicada (puede ser null) */
  nombrePromocion?: string;
  /** Flag para condicionales en UI */
  tienePromocion: boolean;
}

/**
 * Ítem con desglose de descuentos (promo vs. manual).
 * Refleja ItemDescuentoDTO anidado en AplicarDescuentoManualResponse.
 * Usado en la vista de descuentos para transparencia financiera.
 */
export interface ItemDescuento {
  itemId: string;
  nombreProducto: string;
  cantidad: number;
  precioUnitario: number;
  subtotalBruto: number;
  montoDescuentoPromo: number;
  nombrePromocion?: string;
  montoDescuentoManual: number;
  porcentajeDescuentoManual?: number;
  precioFinal: number;
}

// ─── Requests ─────────────────────────────────────────────────────────────────

/**
 * Body HTTP para agregar un producto a un pedido (HU-05).
 * Refleja AgregarProductoRequestBody del backend.
 * El pedidoId viaja como path param.
 */
export interface AgregarProductoRequest {
  productoId: string;
  cantidad: number;
  observaciones?: string;
}

/**
 * Body HTTP para modificar cantidad de un ítem (HU-21).
 * Refleja ModificarCantidadItemRequestBody del backend.
 * El pedidoId e itemPedidoId viajan como path params.
 *
 * @example cantidad = 0 → indica que el usuario desea eliminar el ítem.
 */
export interface ModificarCantidadItemRequest {
  /** 0 = eliminar ítem, > 0 = nueva cantidad */
  cantidad: number;
}

/**
 * Body HTTP para aplicar descuento manual (HU-14).
 * Refleja DescuentoManualRequestBody del backend.
 *
 * El pedidoId y opcionalmente itemPedidoId viajan como path params.
 * Si no se envía itemPedidoId → descuento global sobre el pedido.
 */
export interface DescuentoManualRequest {
  /** Porcentaje del descuento (0–100, máx 2 decimales) */
  porcentaje: number;
  /** Motivo del descuento (ej: "Cliente frecuente", "Compensación por demora") */
  razon?: string;
  /** ID del usuario que aplica el descuento (auditoría) */
  usuarioId: string;
}

// ─── Responses ────────────────────────────────────────────────────────────────

/**
 * Respuesta al agregar un producto al pedido (HU-05).
 * Refleja AgregarProductoResponse del backend.
 *
 * El DTO NO calcula matemáticas propias, confía en el Dominio.
 */
export interface AgregarProductoResponse {
  pedidoId: string;
  numeroPedido: number;
  estadoPedido: string;
  items: ItemPedido[];
  subtotal: number;
  totalDescuentos: number;
  total: number;
  /** ISO 8601 datetime */
  fechaApertura: string;
}

/**
 * Detalle completo del pedido (HU-06).
 * Refleja DetallePedidoResponse del backend.
 *
 * Los descuentos se aplican a nivel ítem (descuentoTotal por ítem)
 * y se resumen en totalDescuentos a nivel pedido.
 */
export interface DetallePedidoResponse {
  pedidoId: string;
  numeroPedido: number;
  numeroMesa: number;
  estado: EstadoPedido;
  /** ISO 8601 datetime */
  fechaApertura: string;
  items: ItemDetalle[];
  /** Total sin descuentos */
  subtotal: number;
  /** Suma de descuentos de todos los ítems */
  totalDescuentos: number;
  /** Lo que paga el cliente (subtotal - totalDescuentos) */
  totalParcial: number;
}

/**
 * Respuesta a la aplicación de descuento manual (HU-14).
 * Refleja AplicarDescuentoManualResponse del backend.
 *
 * Proporciona transparencia sobre el desglose completo de descuentos:
 * promos automáticas, manuales por ítem y global.
 */
export interface AplicarDescuentoManualResponse {
  pedidoId: string;
  items: ItemDescuento[];
  /** Suma de (precioUnitario × cantidad) sin descuentos */
  subtotalBruto: number;
  /** Suma de descuentos de promociones automáticas (HU-10) */
  totalPromocionesAuto: number;
  /** Suma de descuentos manuales por ítem */
  montoDescuentoManualItems: number;
  /** Descuento global aplicado al pedido */
  montoDescuentoGlobal: number;
  /** Total final del pedido */
  totalFinal: number;
  /** Indica si hay descuento global activo */
  tieneDescuentoGlobal: boolean;
}
