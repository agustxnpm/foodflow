package com.agustinpalma.comandas.domain.exception;

import java.time.LocalDate;

/**
 * Excepción de dominio lanzada cuando se intenta cerrar una jornada
 * que ya fue cerrada previamente para la misma fecha operativa.
 * 
 * Garantiza idempotencia: una fecha operativa solo puede cerrarse una vez por local.
 */
public class JornadaYaCerradaException extends RuntimeException {

    private final LocalDate fechaOperativa;

    /**
     * @param fechaOperativa la fecha operativa que ya tiene jornada cerrada
     */
    public JornadaYaCerradaException(LocalDate fechaOperativa) {
        super(String.format(
            "La jornada del %s ya fue cerrada previamente. No se puede cerrar dos veces.",
            fechaOperativa
        ));
        this.fechaOperativa = fechaOperativa;
    }

    public LocalDate getFechaOperativa() {
        return fechaOperativa;
    }
}
