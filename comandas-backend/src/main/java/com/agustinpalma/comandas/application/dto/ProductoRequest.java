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
    
    String colorHex  // Opcional, si es null el dominio asigna #FFFFFF
) {
}
