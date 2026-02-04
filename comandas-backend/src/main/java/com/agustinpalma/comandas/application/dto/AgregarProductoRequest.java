package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;

/**
 * DTO de entrada para el caso de uso AgregarProducto.
 * Representa la intención de agregar un producto a un pedido existente.
 * 
 * HU-05: Agregar productos a un pedido
 */
public record AgregarProductoRequest(
    PedidoId pedidoId,      // ID del pedido al que se agregará el producto
    ProductoId productoId,  // ID del producto a agregar
    int cantidad,           // Cantidad de unidades (debe ser > 0)
    String observaciones    // Notas adicionales (ej: "sin cebolla"), puede ser null
) {
    public AgregarProductoRequest {
        if (pedidoId == null) {
            throw new IllegalArgumentException("El pedidoId es obligatorio");
        }
        if (productoId == null) {
            throw new IllegalArgumentException("El productoId es obligatorio");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
    }
}
