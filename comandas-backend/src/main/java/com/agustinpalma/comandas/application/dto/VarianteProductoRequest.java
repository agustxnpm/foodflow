package com.agustinpalma.comandas.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

/**
 * DTO de entrada para crear una variante de un producto existente.
 * 
 * Una variante es un producto hermano que comparte el mismo concepto base
 * pero difiere en jerarquía (ej: Simple → Doble → Triple).
 * 
 * El productoBaseId viene del path de la URL, no del body.
 * El grupoVarianteId se determina automáticamente en el use case.
 */
public record VarianteProductoRequest(

    @NotBlank(message = "El nombre de la variante es obligatorio")
    String nombre,

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
    BigDecimal precio,

    @NotNull(message = "La cantidad de discos es obligatoria para definir la jerarquía de la variante")
    @Min(value = 1, message = "La cantidad de discos debe ser al menos 1")
    Integer cantidadDiscosCarne,

    Boolean activo,  // Opcional, default true en creación

    String colorHex,  // Opcional, hereda del base si es null

    String categoriaId,  // Opcional, hereda del base si es null

    Boolean permiteExtras,  // Opcional, hereda del base si es null

    Boolean requiereConfiguracion,  // Opcional, hereda del base si es null

    Boolean controlaStock  // Opcional, default false en creación
) {
}
