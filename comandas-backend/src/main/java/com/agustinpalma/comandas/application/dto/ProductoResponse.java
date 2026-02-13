package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.Producto;
import java.math.BigDecimal;

/**
 * DTO de salida para productos.
 * Representa la información de un producto que se expone en la API REST.
 * Incluye el colorHex para permitir al frontend renderizar botones con colores.
 * Incluye información de stock para transparencia del inventario.
 */
public record ProductoResponse(
    String id,              // UUID como String para JSON/REST
    String nombre,
    BigDecimal precio,
    boolean activo,
    String colorHex,        // Siempre normalizado a mayúsculas (ej: #FF0000)
    Integer stockActual,    // Cantidad actual en inventario
    Boolean controlaStock   // Si el producto tiene control de inventario activo
) {
    /**
     * Factory method para construir el DTO desde la entidad de dominio.
     */
    public static ProductoResponse fromDomain(Producto producto) {
        return new ProductoResponse(
            producto.getId().getValue().toString(),
            producto.getNombre(),
            producto.getPrecio(),
            producto.isActivo(),
            producto.getColorHex(),
            producto.getStockActual(),
            producto.isControlaStock()
        );
    }
}
