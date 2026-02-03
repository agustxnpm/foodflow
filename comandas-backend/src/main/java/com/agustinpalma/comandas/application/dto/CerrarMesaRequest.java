package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago;

/**
 * DTO de entrada para el caso de uso de cerrar mesa.
 * 
 * @param mesaId identificador de la mesa a cerrar
 * @param medioPago medio de pago utilizado para el cierre
 */
public record CerrarMesaRequest(
    String mesaId,
    MedioPago medioPago
) {
}
