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

    /**
     * Tipo de movimiento de caja.
     * EGRESO: salida de efectivo (compras, reparaciones, etc.)
     * INGRESO: entrada manual de efectivo (plataformas externas, ajustes, etc.)
     */
    public enum TipoMovimiento {
        EGRESO,
        INGRESO
    }

    /**
     * Estado de una jornada de caja.
     * ABIERTA: la caja fue abierta explícitamente con un fondo inicial declarado.
     * CERRADA: la jornada fue cerrada como snapshot contable del día.
     *
     * Ciclo de vida: ABIERTA → CERRADA (irreversible).
     */
    public enum EstadoJornada {
        ABIERTA,
        CERRADA
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

    /**
     * HU-22: Tipos de movimiento de stock.
     * Cada movimiento registra la razón del cambio en el inventario.
     */
    public enum TipoMovimientoStock {
        VENTA,
        REAPERTURA_PEDIDO,
        AJUSTE_MANUAL,
        INGRESO_MERCADERIA
    }
}
