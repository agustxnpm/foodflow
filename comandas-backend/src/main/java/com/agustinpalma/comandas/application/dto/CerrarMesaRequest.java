package com.agustinpalma.comandas.application.dto;

import java.util.List;

/**
 * DTO de entrada para el caso de uso de cerrar mesa.
 * Soporta pagos parciales (split) mediante una lista de PagoRequest.
 * 
 * @param mesaId identificador de la mesa a cerrar
 * @param pagos lista de pagos para cubrir el total del pedido
 */
public record CerrarMesaRequest(
    String mesaId,
    List<PagoRequest> pagos
) {
}
