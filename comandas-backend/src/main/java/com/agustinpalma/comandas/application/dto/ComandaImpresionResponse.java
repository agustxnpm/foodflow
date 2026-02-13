package com.agustinpalma.comandas.application.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HU-05: DTO de solo lectura para impresión de comanda operativa (cocina/barra).
 *
 * Contiene únicamente información operativa: qué preparar, cantidades y observaciones.
 * NO incluye valores monetarios — la cocina no necesita precios.
 *
 * Inmutable por diseño (Java Record). Validado en construcción (fail-fast).
 */
public record ComandaImpresionResponse(
        HeaderComanda header,
        List<ItemComanda> items
) {

    public ComandaImpresionResponse {
        if (header == null) {
            throw new IllegalArgumentException("El header de la comanda no puede ser nulo");
        }
        if (items == null) {
            throw new IllegalArgumentException("La lista de ítems de la comanda no puede ser nula");
        }
    }

    public record HeaderComanda(
            int numeroMesa,
            int numeroPedido,
            LocalDateTime fechaHora
    ) {
        public HeaderComanda {
            if (numeroMesa <= 0) {
                throw new IllegalArgumentException("El número de mesa debe ser mayor a cero");
            }
            if (numeroPedido <= 0) {
                throw new IllegalArgumentException("El número de pedido debe ser mayor a cero");
            }
            if (fechaHora == null) {
                throw new IllegalArgumentException("La fecha/hora no puede ser nula");
            }
        }
    }

    public record ItemComanda(
            int cantidad,
            String nombreProducto,
            String observaciones
    ) {
        public ItemComanda {
            if (cantidad <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
            }
            if (nombreProducto == null || nombreProducto.isBlank()) {
                throw new IllegalArgumentException("El nombre del producto no puede ser nulo o vacío");
            }
        }
    }
}
