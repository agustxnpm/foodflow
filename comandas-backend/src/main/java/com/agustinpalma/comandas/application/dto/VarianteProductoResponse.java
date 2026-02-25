package com.agustinpalma.comandas.application.dto;

import java.util.List;

/**
 * DTO de salida para la creación de variantes.
 * 
 * Incluye la variante recién creada y todas las variantes hermanas del grupo,
 * permitiendo al consumidor visualizar el grupo completo de variantes.
 */
public record VarianteProductoResponse(
    /** La variante recién creada */
    ProductoResponse varianteCreada,
    /** Todas las variantes del grupo (incluye la base y la nueva) */
    List<ProductoResponse> variantesDelGrupo
) {
}
