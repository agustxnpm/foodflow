package com.agustinpalma.comandas.domain.exception;

/**
 * Excepción de dominio lanzada cuando se intenta agregar un extra
 * de tipo modificador estructural (ej: disco de carne) a un ítem
 * que NO está en la variante estructural máxima de su grupo.
 * 
 * Regla de negocio:
 *   puedeAgregarDiscoExtra = (cantidadDiscosActual == maxEstructural)
 * 
 * Si cantidadDiscosActual < maxEstructural, el usuario debe escalar
 * la variante primero (ej: Simple → Doble) en lugar de agregar el
 * modificador como extra suelto.
 * 
 * Ejemplo: No se puede agregar "Disco de Carne" extra a una hamburguesa
 * Simple si existe la variante Doble en el catálogo.
 */
public class DiscoExtraNoPermitidoException extends RuntimeException {

    public DiscoExtraNoPermitidoException(String message) {
        super(message);
    }
}
