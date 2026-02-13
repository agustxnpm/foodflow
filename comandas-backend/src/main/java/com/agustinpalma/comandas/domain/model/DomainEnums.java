package com.agustinpalma.comandas.domain.model;

/**
 * Enumeraciones del dominio.
 * Definen estados, tipos y catálogos de valores válidos.
 */
public final class DomainEnums {

    private DomainEnums() {
        throw new UnsupportedOperationException("Clase de utilidad no instanciable");
    }

    public enum EstadoMesa {
        LIBRE,
        ABIERTA
    }

    public enum EstadoPedido {
        ABIERTO,
        CERRADO
    }

    public enum MedioPago {
        EFECTIVO,
        TARJETA,
        TRANSFERENCIA,
        QR,
        A_CUENTA
    }

    public enum TipoEstrategia {
        DESCUENTO_DIRECTO,
        CANTIDAD_FIJA,
        COMBO_CONDICIONAL,
        PRECIO_FIJO_CANTIDAD
    }

    public enum ModoDescuento {
        PORCENTAJE,
        MONTO_FIJO
    }

    public enum EstadoPromocion {
        ACTIVA,
        INACTIVA
    }

    public enum TipoDescuento {
        PROMOCION,
        MANUAL
    }

    /**
     * Tipo de movimiento de caja.
     * Actualmente solo soporta EGRESO (salida de efectivo).
     */
    public enum TipoMovimiento {
        EGRESO
    }

    public enum AmbitoDescuento {
        ITEM,
        TOTAL
    }

    public enum TipoCriterio {
        TEMPORAL,
        CONTENIDO,
        MONTO_MINIMO
    }

    /**
     * Tipo de alcance: si la referencia es un producto individual o una categoría.
     * HU-09: Asociar productos a promociones.
     */
    public enum TipoAlcance {
        PRODUCTO,
        CATEGORIA
    }

    /**
     * Rol de un producto/categoría en una promoción.
     * HU-09: Asociar productos a promociones.
     * 
     * - TRIGGER: Activa la promoción (satisface el CriterioContenido)
     * - TARGET: Recibe el beneficio de la estrategia
     */
    public enum RolPromocion {
        TRIGGER,
        TARGET
    }
}
