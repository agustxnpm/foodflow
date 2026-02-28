package com.agustinpalma.comandas.domain.exception;

/**
 * Excepción de dominio lanzada cuando se intenta cerrar la jornada de caja
 * pero existen mesas con estado ABIERTA en el local.
 * 
 * Regla de negocio: todas las mesas deben estar LIBRES antes de cerrar la jornada.
 * El operador debe cerrar todos los pedidos pendientes primero.
 * 
 * El campo {@code mesasAbiertas} permite a la presentación informar al usuario
 * cuántas mesas quedan por cerrar.
 */
public class MesasAbiertasException extends RuntimeException {

    private final int mesasAbiertas;

    /**
     * @param mesasAbiertas cantidad de mesas con estado ABIERTA
     */
    public MesasAbiertasException(int mesasAbiertas) {
        super(String.format(
            "No se puede cerrar la jornada. Existen %d mesa(s) con pedidos abiertos. " +
            "Cierre todos los pedidos antes de cerrar la caja.",
            mesasAbiertas
        ));
        this.mesasAbiertas = mesasAbiertas;
    }

    /**
     * Cantidad de mesas abiertas al momento del intento de cierre.
     */
    public int getMesasAbiertas() {
        return mesasAbiertas;
    }
}
