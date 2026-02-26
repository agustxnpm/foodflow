package com.agustinpalma.comandas.application.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de entrada HTTP para aplicar descuentos manuales.
 * Contiene validaciones Bean Validation para la capa de presentación.
 * 
 * HU-14: Aplicar descuento manual por porcentaje o monto fijo
 * 
 * Validaciones:
 * - tipoDescuento: obligatorio, PORCENTAJE o MONTO_FIJO
 * - valor: obligatorio, mayor a 0, máximo 2 decimales
 * - usuarioId: obligatorio
 */
public record DescuentoManualRequestBody(
    @NotNull(message = "El tipo de descuento es obligatorio")
    @Pattern(regexp = "PORCENTAJE|MONTO_FIJO", message = "El tipo de descuento debe ser PORCENTAJE o MONTO_FIJO")
    String tipoDescuento,

    @NotNull(message = "El valor es obligatorio")
    @DecimalMin(value = "0.01", message = "El valor debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2, message = "El valor debe tener máximo 2 decimales")
    BigDecimal valor,

    @Size(max = 255, message = "La razón no puede exceder 255 caracteres")
    String razon,

    @NotNull(message = "El usuarioId es obligatorio")
    UUID usuarioId
) {}
