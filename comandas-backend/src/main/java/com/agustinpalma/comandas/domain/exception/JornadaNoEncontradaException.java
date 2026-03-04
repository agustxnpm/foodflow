package com.agustinpalma.comandas.domain.exception;

import com.agustinpalma.comandas.domain.model.DomainIds.JornadaCajaId;

/**
 * Excepción de dominio lanzada cuando se busca una jornada de caja
 * por ID y no se encuentra en el sistema.
 *
 * Usada por el caso de uso de generación de PDF cuando el ID proporcionado
 * no corresponde a ninguna jornada cerrada.
 */
public class JornadaNoEncontradaException extends RuntimeException {

    private final JornadaCajaId jornadaId;

    public JornadaNoEncontradaException(JornadaCajaId jornadaId) {
        super(String.format("No se encontró la jornada de caja con ID: %s", jornadaId.getValue()));
        this.jornadaId = jornadaId;
    }

    public JornadaCajaId getJornadaId() {
        return jornadaId;
    }
}
