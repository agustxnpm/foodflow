package com.agustinpalma.comandas.application.dto;

import java.math.BigDecimal;

/**
 * DTO de respuesta para el estado actual de la caja.
 *
 * Dos escenarios posibles:
 *
 * 1. Caja ABIERTA:
 *    { estado: "ABIERTA", jornadaId: "uuid", fondoInicial: 15400.00,
 *      fechaApertura: "2026-03-04T09:00:00", saldoSugerido: null }
 *
 * 2. Caja CERRADA (sin jornada abierta):
 *    { estado: "CERRADA", jornadaId: null, fondoInicial: null,
 *      fechaApertura: null, saldoSugerido: 15400.00 }
 *
 * El campo saldoSugerido contiene el balanceEfectivo de la última
 * jornada cerrada para facilitar la apertura rápida.
 *
 * @param estado        "ABIERTA" o "CERRADA"
 * @param jornadaId     UUID de la jornada abierta (null si CERRADA)
 * @param fondoInicial  monto declarado al abrir (null si CERRADA)
 * @param fechaApertura ISO datetime de la apertura (null si CERRADA)
 * @param saldoSugerido balance de la última jornada cerrada (null si ABIERTA o sin historial)
 */
public record EstadoCajaResponse(
    String estado,
    String jornadaId,
    BigDecimal fondoInicial,
    String fechaApertura,
    BigDecimal saldoSugerido
) {

    /**
     * Factory: caja abierta con datos de la jornada activa.
     */
    public static EstadoCajaResponse abierta(String jornadaId, BigDecimal fondoInicial,
                                              String fechaApertura) {
        return new EstadoCajaResponse("ABIERTA", jornadaId, fondoInicial, fechaApertura, null);
    }

    /**
     * Factory: caja cerrada con sugerencia del último cierre.
     */
    public static EstadoCajaResponse cerrada(BigDecimal saldoSugerido) {
        return new EstadoCajaResponse("CERRADA", null, null, null, saldoSugerido);
    }
}
