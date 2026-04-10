package com.agustinpalma.comandas.presentation.dto;

/**
 * DTO de entrada para la activación del local en el primer arranque.
 *
 * El cliente pega el UUID (código de activación) y completa los datos de su local.
 * Todos los campos opcionales excepto localId y nombreLocal.
 */
public record ActivationRequestDTO(
    String localId,
    String nombreLocal,
    String direccion,
    String telefono,
    String cuit,
    String mensajeBienvenida
) {
    public ActivationRequestDTO {
        if (localId == null || localId.isBlank()) {
            throw new IllegalArgumentException("El código de activación (localId) es obligatorio");
        }
        if (nombreLocal == null || nombreLocal.isBlank()) {
            throw new IllegalArgumentException("El nombre del local es obligatorio");
        }
    }
}
