package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.DomainEnums.TipoMovimientoStock;
import com.agustinpalma.comandas.domain.model.MovimientoStock;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para operaciones de stock.
 * HU-22: Gestión de inventario.
 */
public record AjustarStockResponse(
    UUID productoId,
    String nombreProducto,
    int stockActual,
    int cantidadAjustada,
    TipoMovimientoStock tipo,
    String motivo,
    LocalDateTime fecha
) {
    public static AjustarStockResponse fromDomain(
            com.agustinpalma.comandas.domain.model.Producto producto,
            MovimientoStock movimiento
    ) {
        return new AjustarStockResponse(
            producto.getId().getValue(),
            producto.getNombre(),
            producto.getStockActual(),
            movimiento.getCantidad(),
            movimiento.getTipo(),
            movimiento.getMotivo(),
            movimiento.getFecha()
        );
    }
}
