package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de entrada para la corrección in-place de un pedido cerrado.
 * 
 * Permite corregir cantidades de ítems existentes y reemplazar los pagos
 * sin necesidad de reabrir la mesa (operación segura para el salón).
 * 
 * @param items lista de correcciones por ítem (solo los que cambian)
 * @param pagos nueva lista completa de pagos (reemplaza los existentes)
 */
public record CorreccionPedidoRequest(
    List<ItemCorreccion> items,
    List<PagoCorreccion> pagos
) {
    /**
     * Corrección de un ítem individual.
     * 
     * @param itemId UUID del ítem a corregir
     * @param cantidad nueva cantidad (0 = eliminar el ítem)
     */
    public record ItemCorreccion(
        String itemId,
        int cantidad
    ) {}

    /**
     * Pago corregido.
     * 
     * @param medio medio de pago (EFECTIVO, TARJETA, etc.)
     * @param monto monto del pago
     */
    public record PagoCorreccion(
        MedioPago medio,
        BigDecimal monto
    ) {}
}
