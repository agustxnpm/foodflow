package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.DomainEnums.TipoMovimientoStock;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;

/**
 * DTO de entrada para ajuste manual de stock.
 * HU-22: Gestión de inventario.
 */
public record AjustarStockRequest(
    ProductoId productoId,
    int cantidad,
    TipoMovimientoStock tipo,
    String motivo
) {
    public AjustarStockRequest {
        if (productoId == null) {
            throw new IllegalArgumentException("El productoId es obligatorio");
        }
        if (cantidad == 0) {
            throw new IllegalArgumentException("La cantidad no puede ser cero");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de movimiento es obligatorio");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo es obligatorio");
        }
    }
}
