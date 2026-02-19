package com.agustinpalma.comandas.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value Object que representa un ajuste económico explícito dentro de un pedido.
 * 
 * Cada instancia describe UN motivo concreto de descuento:
 * qué tipo es, a qué nivel aplica, por qué se aplicó y cuánto vale.
 * 
 * El Pedido materializa estos ajustes a partir de sus promociones y descuentos
 * manuales, convirtiendo al agregado en la única fuente de verdad del relato económico.
 * 
 * Inmutable por diseño. El monto es un snapshot monetario final (no un porcentaje).
 * 
 * Ejemplos:
 * - AjusteEconomico(PROMOCION, ITEM, "2x1 Cervezas", $1500)
 * - AjusteEconomico(MANUAL, ITEM, "Producto quemado", $300)
 * - AjusteEconomico(MANUAL, TOTAL, "Cortesía", $230)
 */
public final class AjusteEconomico {

    /**
     * Tipo de ajuste según su origen.
     */
    public enum TipoAjuste {
        /** Descuento originado por una promoción automática (HU-10) */
        PROMOCION,
        /** Descuento aplicado manualmente por el operador (HU-14) */
        MANUAL
    }

    /**
     * Ámbito de aplicación del ajuste.
     */
    public enum AmbitoAjuste {
        /** Ajuste aplicado a un ítem específico */
        ITEM,
        /** Ajuste aplicado al total del pedido */
        TOTAL
    }

    private final TipoAjuste tipo;
    private final AmbitoAjuste ambito;
    private final String descripcion;
    private final BigDecimal monto;

    /**
     * @param tipo     origen del ajuste (PROMOCION o MANUAL)
     * @param ambito   nivel de aplicación (ITEM o TOTAL)
     * @param descripcion razón legible para el ticket (ej: "2x1 Cervezas", "Cortesía")
     * @param monto    valor monetario final del descuento (siempre >= 0)
     */
    public AjusteEconomico(TipoAjuste tipo, AmbitoAjuste ambito, String descripcion, BigDecimal monto) {
        this.tipo = Objects.requireNonNull(tipo, "El tipo de ajuste no puede ser null");
        this.ambito = Objects.requireNonNull(ambito, "El ámbito del ajuste no puede ser null");
        this.descripcion = Objects.requireNonNull(descripcion, "La descripción no puede ser null");

        Objects.requireNonNull(monto, "El monto no puede ser null");
        if (monto.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                String.format("El monto del ajuste no puede ser negativo: %s", monto)
            );
        }
        this.monto = monto;
    }

    public TipoAjuste getTipo() {
        return tipo;
    }

    public AmbitoAjuste getAmbito() {
        return ambito;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AjusteEconomico that = (AjusteEconomico) o;
        return tipo == that.tipo
            && ambito == that.ambito
            && Objects.equals(descripcion, that.descripcion)
            && monto.compareTo(that.monto) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tipo, ambito, descripcion, monto);
    }

    @Override
    public String toString() {
        return String.format("AjusteEconomico{%s, %s, '%s', $%s}", tipo, ambito, descripcion, monto);
    }
}
