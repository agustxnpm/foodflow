package com.agustinpalma.comandas.application.dto;

import java.math.BigDecimal;

/**
 * Request body para registrar un egreso de caja.
 * Recibido desde el controller REST.
 *
 * @param monto monto del egreso (debe ser > 0)
 * @param descripcion descripción del egreso (ej: "Productos de limpieza")
 */
public record EgresoRequestBody(
    BigDecimal monto,
    String descripcion
) {}
