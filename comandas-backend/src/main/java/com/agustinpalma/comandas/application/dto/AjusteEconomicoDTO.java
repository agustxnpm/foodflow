package com.agustinpalma.comandas.application.dto;

import java.math.BigDecimal;

/**
 * DTO que transporta un ajuste económico del dominio hacia la presentación.
 * 
 * Cada instancia explica un motivo concreto de descuento con su monto final.
 * Elimina la necesidad de inferir descuentos por resta subtotal - total.
 * 
 * El frontend recibe esta lista y puede renderizar el desglose completo
 * sin recalcular ni deducir valores.
 */
public record AjusteEconomicoDTO(
    String tipo,        // "PROMOCION" | "MANUAL"
    String ambito,      // "ITEM" | "TOTAL"
    String descripcion,
    BigDecimal monto
) {
    public AjusteEconomicoDTO {
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("El tipo no puede ser nulo o vacío");
        }
        if (ambito == null || ambito.isBlank()) {
            throw new IllegalArgumentException("El ámbito no puede ser nulo o vacío");
        }
        if (descripcion == null) {
            throw new IllegalArgumentException("La descripción no puede ser nula");
        }
        if (monto == null || monto.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto no puede ser nulo ni negativo");
        }
    }
}
