package com.agustinpalma.comandas.application.dto;

import java.math.BigDecimal;

/**
 * DTO que representa un ítem individual dentro del detalle del pedido.
 * Contiene información snapshot del producto y cálculos financieros.
 * 
 * Se usa como parte de la respuesta en la consulta de detalle de pedido.
 */
public record ItemDetalleDTO(
    String id,
    String nombreProducto,
    int cantidad,
    BigDecimal precioUnitario,
    BigDecimal subtotal,
    String observacion
) {
    /**
     * Valida que los campos obligatorios no sean nulos.
     * Se ejecuta automáticamente por el compilador en el constructor canónico.
     */
    public ItemDetalleDTO {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del ítem no puede ser nulo o vacío");
        }
        if (nombreProducto == null || nombreProducto.isBlank()) {
            throw new IllegalArgumentException("El nombre del producto no puede ser nulo o vacío");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
        if (precioUnitario == null || precioUnitario.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio unitario no puede ser nulo ni negativo");
        }
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El subtotal no puede ser nulo ni negativo");
        }
    }
}
