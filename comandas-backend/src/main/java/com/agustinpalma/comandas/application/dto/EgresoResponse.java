package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.MovimientoCaja;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO para un egreso de caja registrado.
 *
 * @param id identificador del movimiento
 * @param monto monto del egreso
 * @param descripcion descripción del egreso
 * @param fecha fecha y hora del registro
 * @param tipo tipo de movimiento (EGRESO)
 * @param numeroComprobante número de comprobante generado automáticamente
 */
public record EgresoResponse(
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
    public static EgresoResponse fromDomain(MovimientoCaja movimiento) {
        return new EgresoResponse(
            movimiento.getId().getValue().toString(),
            movimiento.getMonto(),
            movimiento.getDescripcion(),
            movimiento.getFecha(),
            movimiento.getTipo().name(),
            movimiento.getNumeroComprobante()
        );
    }
}
