package com.agustinpalma.comandas.application.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de entrada HTTP para aplicar descuentos manuales.
 * Contiene validaciones Bean Validation para la capa de presentación.
 * 
 * HU-14: Aplicar descuento manual por porcentaje
 * 
 * Validaciones:
 * - porcentaje: 0-100, obligatorio, máximo 2 decimales
 * - usuarioId: obligatorio
 */
public record DescuentoManualRequestBody(
    @NotNull(message = "El porcentaje es obligatorio")
    @DecimalMin(value = "0.0", message = "El porcentaje debe ser mayor o igual a 0")
    @DecimalMax(value = "100.0", message = "El porcentaje debe ser menor o igual a 100")
    @Digits(integer = 3, fraction = 2, message = "El porcentaje debe tener máximo 2 decimales")
    BigDecimal porcentaje,

    @Size(max = 255, message = "La razón no puede exceder 255 caracteres")
    String razon,

    @NotNull(message = "El usuarioId es obligatorio")
    UUID usuarioId
) {}
