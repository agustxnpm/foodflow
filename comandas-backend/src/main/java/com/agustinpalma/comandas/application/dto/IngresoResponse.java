package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.MovimientoCaja;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO para un ingreso manual de caja registrado.
 *
 * @param id identificador del movimiento
 * @param monto monto del ingreso
 * @param descripcion descripción del ingreso
 * @param fecha fecha y hora del registro
 * @param tipo tipo de movimiento (INGRESO)
 * @param numeroComprobante número de comprobante generado automáticamente (ING-...)
 */
public record IngresoResponse(
    String id,
    BigDecimal monto,
    String descripcion,
    LocalDateTime fecha,
    String tipo,
    String numeroComprobante
) {
    /**
     * Factory method para crear la respuesta desde el modelo de dominio.
     *
     * @param movimiento movimiento de caja del dominio
     * @return DTO de respuesta
     */
    public static IngresoResponse fromDomain(MovimientoCaja movimiento) {
        return new IngresoResponse(
            movimiento.getId().getValue().toString(),
            movimiento.getMonto(),
            movimiento.getDescripcion(),
            movimiento.getFecha(),
            movimiento.getTipo().name(),
            movimiento.getNumeroComprobante()
        );
    }
}
