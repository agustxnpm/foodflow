package com.agustinpalma.comandas.application.dto;

import java.math.BigDecimal;

/**
 * DTO que representa un extra aplicado a un ítem del pedido.
 * Snapshot inmutable del extra al momento de agregarse.
 *
 * Usado en ItemDetalleDTO para que el frontend muestre los extras
 * como sub-elementos del ítem, NO como líneas independientes.
 */
public record ExtraDetalleDTO(
    String productoId,
    String nombre,
    BigDecimal precio
) {
    public ExtraDetalleDTO {
        if (productoId == null || productoId.isBlank()) {
            throw new IllegalArgumentException("El productoId del extra no puede ser nulo o vacío");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del extra no puede ser nulo o vacío");
        }
        if (precio == null || precio.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio del extra no puede ser nulo ni negativo");
        }
    }
}
