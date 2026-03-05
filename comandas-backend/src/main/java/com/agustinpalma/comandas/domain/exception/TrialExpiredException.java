package com.agustinpalma.comandas.domain.exception;

/**
 * Excepción de dominio lanzada cuando el período de prueba del sistema
 * ha expirado y se intenta realizar una operación bloqueada (ej: apertura de jornada).
 *
 * El frontend debe mostrar una pantalla de bloqueo total que indique
 * al operador que debe contactar soporte para activar la licencia.
 *
 * Los datos del local se conservan intactos — solo se bloquea la operación.
 */
public class TrialExpiredException extends RuntimeException {

    public TrialExpiredException() {
        super("El período de prueba ha finalizado. Contacte al soporte técnico para activar su licencia.");
    }
}
