package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.DomainEnums.ModoDescuento;
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
 * Soporta dos tipos de descuento:
 * - PORCENTAJE: valor entre 0.01 y 100
 * - MONTO_FIJO: valor monetario positivo
 * 
 * @param pedidoId ID del pedido al cual aplicar el descuento (obligatorio)
 * @param itemPedidoId ID del ítem específico (opcional, null para descuento global)
 * @param tipoDescuento Tipo de descuento (PORCENTAJE o MONTO_FIJO)
 * @param valor Valor del descuento (porcentaje 0.01–100 o monto fijo positivo)
 * @param razon Motivo del descuento (ej: "Cliente frecuente", "Compensación por demora")
 * @param usuarioId ID del usuario que aplica el descuento (auditoría)
 */
public record AplicarDescuentoManualRequest(
    PedidoId pedidoId,
    ItemPedidoId itemPedidoId,  // null = descuento global
    ModoDescuento tipoDescuento,
    BigDecimal valor,
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
