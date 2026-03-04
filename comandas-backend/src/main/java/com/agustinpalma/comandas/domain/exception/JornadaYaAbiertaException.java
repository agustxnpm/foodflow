package com.agustinpalma.comandas.domain.exception;

/**
 * Excepción de dominio lanzada cuando se intenta abrir una jornada
 * pero ya existe una jornada ABIERTA para el local.
 *
 * Garantiza invariante: máximo una jornada ABIERTA por local.
 */
public class JornadaYaAbiertaException extends RuntimeException {

    public JornadaYaAbiertaException() {
        super("Ya existe una jornada abierta. Cierre la jornada actual antes de abrir una nueva.");
    }
}
