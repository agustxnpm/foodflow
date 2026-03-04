package com.agustinpalma.comandas.application.dto;

import java.math.BigDecimal;

/**
 * Request body para registrar un ingreso manual de caja.
 * Recibido desde el controller REST.
 *
 * Usado para registrar efectivo proveniente de plataformas externas
 * (PedidosYa, Rappi) u otros ingresos que no generan un ticket de mesa.
 *
 * @param monto monto del ingreso (debe ser > 0)
 * @param descripcion descripción del ingreso (ej: "Cobro PedidosYa en efectivo")
 */
public record IngresoRequestBody(
    BigDecimal monto,
    String descripcion
) {}
