package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.Categoria;

/**
 * DTO de salida para categorías.
 * Representa la información de una categoría que se expone en la API REST.
 */
public record CategoriaResponse(
    String id,
    String nombre,
    String colorHex,
    boolean admiteVariantes,
    boolean esCategoriaExtra,
    int orden
) {

    /**
     * Factory method para construir el DTO desde la entidad de dominio.
     */
    public static CategoriaResponse fromDomain(Categoria categoria) {
        return new CategoriaResponse(
            categoria.getId().getValue().toString(),
            categoria.getNombre(),
            categoria.getColorHex(),
            categoria.isAdmiteVariantes(),
            categoria.isEsCategoriaExtra(),
            categoria.getOrden()
        );
    }
}
