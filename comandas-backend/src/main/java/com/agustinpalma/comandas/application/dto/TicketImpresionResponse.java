package com.agustinpalma.comandas.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * HU-29: DTO de solo lectura para impresión de ticket de venta (cliente).
 *
 * Contiene resumen financiero con totales, descuentos y datos del local.
 * Diseñado para renderizado en impresora térmica desde el frontend.
 *
 * Inmutable por diseño (Java Record). Validado en construcción (fail-fast).
 */
public record TicketImpresionResponse(
        HeaderTicket header,
        List<ItemTicket> items,
        TotalesTicket totales,
        FooterTicket footer
) {

    public TicketImpresionResponse {
        if (header == null) {
            throw new IllegalArgumentException("El header del ticket no puede ser nulo");
        }
        if (items == null) {
            throw new IllegalArgumentException("La lista de ítems del ticket no puede ser nula");
        }
        if (totales == null) {
            throw new IllegalArgumentException("Los totales del ticket no pueden ser nulos");
        }
        if (footer == null) {
            throw new IllegalArgumentException("El footer del ticket no puede ser nulo");
        }
    }

    public record HeaderTicket(
            String nombreLocal,
            String direccion,
            String telefono,
            String cuit,
            LocalDateTime fechaHora,
            int numeroMesa
    ) {
        public HeaderTicket {
            if (nombreLocal == null || nombreLocal.isBlank()) {
                throw new IllegalArgumentException("El nombre del local no puede ser nulo o vacío");
            }
            if (fechaHora == null) {
                throw new IllegalArgumentException("La fecha/hora no puede ser nula");
            }
            if (numeroMesa <= 0) {
                throw new IllegalArgumentException("El número de mesa debe ser mayor a cero");
            }
        }
    }

    public record ItemTicket(
            int cantidad,
            String descripcion,
            BigDecimal precioUnitario,
            BigDecimal importe
    ) {
        public ItemTicket {
            if (cantidad <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
            }
            if (descripcion == null || descripcion.isBlank()) {
                throw new IllegalArgumentException("La descripción no puede ser nula o vacía");
            }
            if (precioUnitario == null || precioUnitario.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("El precio unitario no puede ser nulo ni negativo");
            }
            if (importe == null || importe.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("El importe no puede ser nulo ni negativo");
            }
        }
    }

    public record TotalesTicket(
            BigDecimal subtotal,
            BigDecimal montoDescuentoPromos,
            BigDecimal montoDescuentoManual,
            BigDecimal totalFinal,
            List<DesgloseAjuste> desgloseAjustes
    ) {
        public TotalesTicket {
            if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("El subtotal no puede ser nulo ni negativo");
            }
            if (montoDescuentoPromos == null || montoDescuentoPromos.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("El monto de descuento por promos no puede ser nulo ni negativo");
            }
            if (montoDescuentoManual == null || montoDescuentoManual.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("El monto de descuento manual no puede ser nulo ni negativo");
            }
            if (totalFinal == null || totalFinal.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("El total final no puede ser nulo ni negativo");
            }
            if (desgloseAjustes == null) {
                throw new IllegalArgumentException("El desglose de ajustes no puede ser nulo");
            }
        }
    }

    /**
     * Desglose de un ajuste económico individual en el ticket.
     * Reemplaza el antiguo DesglosePromocion con soporte para cualquier tipo de descuento.
     */
    public record DesgloseAjuste(
            String tipo,
            String descripcion,
            BigDecimal monto
    ) {
        public DesgloseAjuste {
            if (tipo == null || tipo.isBlank()) {
                throw new IllegalArgumentException("El tipo de ajuste no puede ser nulo o vacío");
            }
            if (descripcion == null) {
                throw new IllegalArgumentException("La descripción del ajuste no puede ser nula");
            }
            if (monto == null || monto.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("El monto no puede ser nulo ni negativo");
            }
        }
    }

    public record FooterTicket(
            String mensajeBienvenida
    ) {
    }
}
