package com.agustinpalma.comandas.application.dto;

import java.math.BigDecimal;

/**
 * DTO de respuesta al abrir una jornada de caja.
 *
 * @param jornadaId      UUID de la jornada recién creada
 * @param fondoInicial   monto inicial declarado
 * @param fechaOperativa fecha operativa calculada (YYYY-MM-DD)
 */
public record AbrirCajaResponse(
    String jornadaId,
    BigDecimal fondoInicial,
    String fechaOperativa
) {
}
