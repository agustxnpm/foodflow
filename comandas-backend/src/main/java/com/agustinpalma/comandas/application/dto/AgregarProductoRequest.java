package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import java.util.List;

/**
 * DTO de entrada para el caso de uso AgregarProducto.
 * Representa la intención de agregar un producto a un pedido existente.
 * 
 * HU-05: Agregar productos a un pedido
 * HU-05.1 + HU-22: Soporte para extras controlados y selección de variante
 */
public record AgregarProductoRequest(
    PedidoId pedidoId,         // ID del pedido al que se agregará el producto
    ProductoId productoId,     // ID del producto base a agregar
    int cantidad,              // Cantidad de unidades (debe ser > 0)
    String observaciones,      // Notas adicionales (ej: "sin cebolla"), puede ser null
    List<ProductoId> extrasIds, // IDs de extras a agregar (opcional, puede ser null o vacío)
    ProductoId varianteId      // ID de la variante seleccionada explícitamente (null = auto-normalización)
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
        // extrasIds puede ser null o vacío
        // varianteId puede ser null (auto-normalización)
    }
    
    /**
     * Constructor de retrocompatibilidad (sin extras ni variante).
     * Facilita migración de código existente.
     */
    public AgregarProductoRequest(
        PedidoId pedidoId,
        ProductoId productoId,
        int cantidad,
        String observaciones
    ) {
        this(pedidoId, productoId, cantidad, observaciones, null, null);
    }

    /**
     * Constructor de retrocompatibilidad (sin variante).
     * Facilita migración de código existente.
     */
    public AgregarProductoRequest(
        PedidoId pedidoId,
        ProductoId productoId,
        int cantidad,
        String observaciones,
        List<ProductoId> extrasIds
    ) {
        this(pedidoId, productoId, cantidad, observaciones, extrasIds, null);
    }
}
