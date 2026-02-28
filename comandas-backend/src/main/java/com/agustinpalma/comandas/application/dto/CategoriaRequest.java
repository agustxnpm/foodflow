package com.agustinpalma.comandas.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada para crear o editar una categoría.
 * Valida los datos básicos a nivel de presentación.
 * Las validaciones de negocio se ejecutan en el dominio.
 */
public record CategoriaRequest(

    @NotBlank(message = "El nombre de la categoría es obligatorio")
    String nombre,

    String colorHex,  // Opcional, si es null el dominio asigna #FFFFFF

    Boolean admiteVariantes,  // Opcional, default false en creación

    Boolean esCategoriaExtra,  // Opcional, default false en creación

    Integer orden,  // Opcional, default 0 en creación

    /** UUID de la categoría de modificadores asociada. Nullable. */
    String categoriaModificadoresId
) {
}
