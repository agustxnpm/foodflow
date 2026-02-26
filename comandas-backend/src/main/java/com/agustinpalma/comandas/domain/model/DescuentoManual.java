package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainEnums.ModoDescuento;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Value Object que representa un descuento manual aplicado a un pedido o ítem.
 * 
 * HU-14: Aplicar descuento inmediato por porcentaje o monto fijo.
 * 
 * A diferencia de las promociones automáticas (HU-10) que se capturan como snapshot inmutable,
 * el descuento manual es DINÁMICO: se guarda el tipo y valor, y el monto se recalcula cada vez
 * que cambia la base gravable.
 * 
 * Tipos soportados:
 * - PORCENTAJE: El valor representa un porcentaje (0.01 – 100). Se calcula: base × valor / 100.
 * - MONTO_FIJO: El valor representa un monto en dinero. Se aplica directamente (capeado a la base gravable).
 * 
 * Características:
 * - Inmutable (Value Object)
 * - Solo guarda el tipo y valor, NO el monto calculado
 * - Tiene lógica para calcular el monto dinámicamente según el tipo
 * - Auditable (registra quién y cuándo lo aplicó)
 * 
 * Regla de Oro: El descuento manual siempre se aplica DESPUÉS de las promociones automáticas.
 * Es decir, se calcula sobre el precio que ya tiene descontadas las promos de la HU-10.
 */
public final class DescuentoManual {

    private static final BigDecimal PORCENTAJE_MAXIMO = new BigDecimal("100");
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final ModoDescuento tipo;
    private final BigDecimal valor;
    private final String razon;
    private final UUID usuarioId;
    private final LocalDateTime fechaAplicacion;

    /**
     * Constructor principal con validación estricta.
     * 
     * @param tipo Tipo de descuento (PORCENTAJE o MONTO_FIJO)
     * @param valor Valor del descuento. Si PORCENTAJE: 0.01–100. Si MONTO_FIJO: mayor a 0.
     * @param razon Motivo del descuento (ej: "Cliente frecuente", "Descuento especial"). Puede estar vacío.
     * @param usuarioId ID del usuario que aplicó el descuento (auditoría)
     * @param fechaAplicacion Momento en que se aplicó el descuento
     * @throws IllegalArgumentException si el valor está fuera del rango válido según el tipo
     * @throws IllegalArgumentException si la razón es nula
     * @throws IllegalArgumentException si usuarioId o fechaAplicacion son nulos
     */
    public DescuentoManual(
            ModoDescuento tipo,
            BigDecimal valor, 
            String razon, 
            UUID usuarioId, 
            LocalDateTime fechaAplicacion
    ) {
        // Validar tipo
        Objects.requireNonNull(tipo, "El tipo de descuento no puede ser null");

        // Validar valor según tipo
        Objects.requireNonNull(valor, "El valor del descuento no puede ser null");
        if (tipo == ModoDescuento.PORCENTAJE) {
            if (valor.compareTo(BigDecimal.ZERO) <= 0 || valor.compareTo(PORCENTAJE_MAXIMO) > 0) {
                throw new IllegalArgumentException(
                    String.format("El porcentaje debe estar entre 0.01 y 100. Recibido: %s", valor)
                );
            }
        } else {
            // MONTO_FIJO: debe ser positivo (la validación contra la base gravable se hace en el aggregate)
            if (valor.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                    String.format("El monto fijo debe ser mayor a 0. Recibido: %s", valor)
                );
            }
        }

        // Validar razón (puede estar vacía, no puede ser null)
        Objects.requireNonNull(razon, "La razón del descuento no puede ser null");

        // Validar auditoría
        Objects.requireNonNull(usuarioId, "El usuarioId no puede ser null");
        Objects.requireNonNull(fechaAplicacion, "La fechaAplicacion no puede ser null");

        this.tipo = tipo;
        this.valor = valor;
        this.razon = razon;  // Se permite vacío, solo se valida que no sea null
        this.usuarioId = usuarioId;
        this.fechaAplicacion = fechaAplicacion;
    }

    /**
     * Calcula el monto del descuento sobre una base gravable.
     * 
     * Este método centraliza la lógica de cálculo y garantiza que el dinamismo funcione.
     * 
     * Según el tipo:
     * - PORCENTAJE: monto = base × (valor / 100). Ej: base=$1000, valor=10 → monto=$100
     * - MONTO_FIJO: monto = min(valor, base). Se capea a la base para evitar totales negativos.
     * 
     * @param baseGravable Base sobre la cual calcular el descuento (puede ser subtotal de ítem o total de pedido)
     * @return Monto del descuento en valor absoluto (siempre >= 0, nunca mayor a la base)
     * @throws IllegalArgumentException si la base es negativa
     */
    public BigDecimal calcularMonto(BigDecimal baseGravable) {
        Objects.requireNonNull(baseGravable, "La base gravable no puede ser null");
        
        if (baseGravable.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                String.format("La base gravable no puede ser negativa: %s", baseGravable)
            );
        }

        if (tipo == ModoDescuento.PORCENTAJE) {
            // Fórmula: monto = base * (valor / 100)
            // Redondeo: HALF_UP para evitar pérdida de centavos
            return baseGravable
                    .multiply(valor)
                    .divide(CIEN, 2, RoundingMode.HALF_UP);
        } else {
            // MONTO_FIJO: aplicar directamente, capeado a la base gravable
            return valor.min(baseGravable);
        }
    }

    /**
     * Retorna el tipo de descuento.
     * 
     * @return PORCENTAJE o MONTO_FIJO
     */
    public ModoDescuento getTipo() {
        return tipo;
    }

    /**
     * Retorna el valor del descuento.
     * Si es PORCENTAJE: valor entre 0.01 y 100.
     * Si es MONTO_FIJO: valor monetario positivo.
     * 
     * @return Valor del descuento
     */
    public BigDecimal getValor() {
        return valor;
    }

    /**
     * Retorna la razón del descuento.
     * 
     * @return Motivo del descuento
     */
    public String getRazon() {
        return razon;
    }

    /**
     * Retorna el ID del usuario que aplicó el descuento.
     * 
     * @return UUID del usuario (auditoría)
     */
    public UUID getUsuarioId() {
        return usuarioId;
    }

    /**
     * Retorna la fecha de aplicación del descuento.
     * 
     * @return LocalDateTime de aplicación (auditoría)
     */
    public LocalDateTime getFechaAplicacion() {
        return fechaAplicacion;
    }

    // ========================================
    // Value Object: equals, hashCode, toString
    // ========================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DescuentoManual that = (DescuentoManual) o;
        return tipo == that.tipo &&
               Objects.equals(valor, that.valor) &&
               Objects.equals(razon, that.razon) &&
               Objects.equals(usuarioId, that.usuarioId) &&
               Objects.equals(fechaAplicacion, that.fechaAplicacion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tipo, valor, razon, usuarioId, fechaAplicacion);
    }

    @Override
    public String toString() {
        String sufijo = tipo == ModoDescuento.PORCENTAJE ? "%" : "$";
        return String.format(
            "DescuentoManual{tipo=%s, valor=%s%s, razon='%s', usuarioId=%s, fecha=%s}",
            tipo, valor, sufijo, razon, usuarioId, fechaAplicacion
        );
    }
}
