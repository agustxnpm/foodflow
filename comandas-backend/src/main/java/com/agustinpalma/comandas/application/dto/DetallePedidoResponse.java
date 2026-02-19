package com.agustinpalma.comandas.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.agustinpalma.comandas.application.dto.AjusteEconomicoDTO;

/**
 * DTO de respuesta para la consulta de detalle de un pedido.
 * 
 * HU-10: Incluye campos de descuentos aplicados:
 * - subtotal: total sin descuentos
 * - totalDescuentos: suma de todos los descuentos
 * - totalParcial: lo que paga el cliente (subtotal - descuentos)
 */
public record DetallePedidoResponse(
    String pedidoId,
    int numeroPedido,
    int numeroMesa,
    String estado,
    LocalDateTime fechaApertura,
    List<ItemDetalleDTO> items,
    BigDecimal subtotal,           // Total sin descuentos
    BigDecimal totalDescuentos,    // Suma de descuentos de todos los ítems
    BigDecimal totalParcial,       // Lo que paga el cliente
    List<AjusteEconomicoDTO> ajustesEconomicos  // Narrativa económica explícita
) {
    /**
     * Valida que los campos obligatorios no sean nulos.
     * Se ejecuta automáticamente por el compilador en el constructor canónico.
     */
    public DetallePedidoResponse {
        if (pedidoId == null || pedidoId.isBlank()) {
            throw new IllegalArgumentException("El pedidoId no puede ser nulo o vacío");
        }
        if (numeroPedido <= 0) {
            throw new IllegalArgumentException("El número de pedido debe ser mayor a cero");
        }
        if (numeroMesa <= 0) {
            throw new IllegalArgumentException("El número de mesa debe ser mayor a cero");
        }
        if (estado == null || estado.isBlank()) {
            throw new IllegalArgumentException("El estado no puede ser nulo o vacío");
        }
        if (fechaApertura == null) {
            throw new IllegalArgumentException("La fecha de apertura no puede ser nula");
        }
        if (items == null) {
            throw new IllegalArgumentException("La lista de ítems no puede ser nula");
        }
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El subtotal no puede ser nulo ni negativo");
        }
        if (totalDescuentos == null || totalDescuentos.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El total de descuentos no puede ser nulo ni negativo");
        }
        if (totalParcial == null || totalParcial.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El total parcial no puede ser nulo ni negativo");
        }
        if (ajustesEconomicos == null) {
            throw new IllegalArgumentException("La lista de ajustes económicos no puede ser nula");
        }
    }
}
