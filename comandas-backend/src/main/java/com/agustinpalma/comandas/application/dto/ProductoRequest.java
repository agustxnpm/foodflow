package com.agustinpalma.comandas.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO de entrada para crear o editar un producto.
 * Valida los datos básicos a nivel de presentación.
 * Las validaciones de negocio se ejecutan en el dominio.
 */
public record ProductoRequest(
    
    @NotBlank(message = "El nombre del producto es obligatorio")
    String nombre,
    
    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
    BigDecimal precio,
    
    Boolean activo,  // Opcional, si es null se asume true en creación
    
    String colorHex,  // Opcional, si es null el dominio asigna #FFFFFF
    
    Boolean controlaStock,  // Opcional, si es null se preserva el estado actual (edición) o false (creación)

    Boolean esExtra,  // Indica si el producto es un extra (huevo, queso, disco, etc.). Default: false en creación

    Boolean esModificadorEstructural,  // Si true, este extra activa normalización de variantes. Default: false en creación

    String categoriaId,  // UUID de la categoría del catálogo. Nullable.

    Boolean permiteExtras,  // Si el producto acepta extras/agregados. Default: true en creación

    Boolean requiereConfiguracion  // Si el POS debe abrir modal de configuración. Default: true en creación
) {
}
