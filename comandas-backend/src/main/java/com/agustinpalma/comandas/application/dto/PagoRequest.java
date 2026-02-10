package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago;

import java.math.BigDecimal;

/**
 * DTO de entrada para un pago individual dentro del cierre de mesa.
 * 
 * @param medio medio de pago utilizado (EFECTIVO, TARJETA, TRANSFERENCIA, QR)
 * @param monto monto del pago (debe ser > 0)
 */
public record PagoRequest(
    MedioPago medio,
    BigDecimal monto
) {
}
