package com.agustinpalma.comandas.presentation.dto;

/**
 * DTO de entrada para actualizar los datos del local (post-activación).
 *
 * No incluye localId — la identidad no se puede cambiar después de la activación.
 * El nombreLocal es obligatorio; el resto es opcional.
 */
public record UpdateLocalDataDTO(
    String nombreLocal,
    String direccion,
    String telefono,
    String cuit,
    String mensajeBienvenida
) {
    public UpdateLocalDataDTO {
        if (nombreLocal == null || nombreLocal.isBlank()) {
            throw new IllegalArgumentException("El nombre del local es obligatorio");
        }
    }
}
