package com.agustinpalma.comandas.application.dto;

import java.math.BigDecimal;

/**
 * DTO de request para abrir una nueva jornada de caja.
 *
 * @param montoInicial efectivo físico declarado en el cajón (≥ 0)
 */
public record AbrirCajaRequest(
    BigDecimal montoInicial
) {
}
