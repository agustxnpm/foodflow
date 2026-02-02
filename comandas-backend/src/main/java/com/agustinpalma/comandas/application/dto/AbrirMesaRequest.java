package com.agustinpalma.comandas.application.dto;

/**
 * DTO de entrada para el caso de uso AbrirMesa.
 * Representa la intención del usuario de abrir una mesa específica.
 */
public record AbrirMesaRequest(
    String mesaId     // ID de la mesa a abrir (viene como String desde REST)
) {
    public AbrirMesaRequest {
        if (mesaId == null || mesaId.isBlank()) {
            throw new IllegalArgumentException("El mesaId es obligatorio");
        }
    }
}
