package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.DomainIds.ItemPedidoId;
import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de entrada para aplicar descuento manual (HU-14).
 * 
 * Soporta dos modalidades:
 * - Descuento por ítem: requiere itemPedidoId
 * - Descuento global: itemPedidoId debe ser null
 * 
 * @param pedidoId ID del pedido al cual aplicar el descuento (obligatorio)
 * @param itemPedidoId ID del ítem específico (opcional, null para descuento global)
 * @param porcentaje Porcentaje del descuento (0-100)
 * @param razon Motivo del descuento (ej: "Cliente frecuente", "Compensación por demora")
 * @param usuarioId ID del usuario que aplica el descuento (auditoría)
 */
public record AplicarDescuentoManualRequest(
    PedidoId pedidoId,
    ItemPedidoId itemPedidoId,  // null = descuento global
    BigDecimal porcentaje,
    String razon,
    UUID usuarioId
) {
    /**
     * Indica si este descuento es global (afecta todo el pedido).
     * 
     * @return true si itemPedidoId es null
     */
    public boolean esDescuentoGlobal() {
        return itemPedidoId == null;
    }

    /**
     * Indica si este descuento es por ítem (afecta un producto específico).
     * 
     * @return true si itemPedidoId no es null
     */
    public boolean esDescuentoPorItem() {
        return itemPedidoId != null;
    }
}
