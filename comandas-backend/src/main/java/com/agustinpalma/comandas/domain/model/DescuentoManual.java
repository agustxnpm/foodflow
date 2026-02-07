package com.agustinpalma.comandas.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Value Object que representa un descuento manual aplicado por porcentaje.
 * 
 * HU-14: Aplicar descuento inmediato por porcentaje.
 * 
 * A diferencia de las promociones automáticas (HU-10) que se capturan como snapshot inmutable,
 * el descuento manual es DINÁMICO: se guarda el porcentaje y el monto se recalcula cada vez
 * que cambia la base gravable.
 * 
 * Características:
 * - Inmutable (Value Object)
 * - Solo guarda el porcentaje, NO el monto calculado
 * - Tiene lógica para calcular el monto dinámicamente
 * - Auditable (registra quién y cuándo lo aplicó)
 * 
 * Regla de Oro: El descuento manual siempre se aplica DESPUÉS de las promociones automáticas.
 * Es decir, se calcula sobre el precio que ya tiene descontadas las promos de la HU-10.
 */
public final class DescuentoManual {

    private static final BigDecimal PORCENTAJE_MINIMO = BigDecimal.ZERO;
    private static final BigDecimal PORCENTAJE_MAXIMO = new BigDecimal("100");
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final BigDecimal porcentaje;
    private final String razon;
    private final UUID usuarioId;
    private final LocalDateTime fechaAplicacion;

    /**
     * Constructor principal con validación estricta.
     * 
     * @param porcentaje Porcentaje del descuento (0-100). Ejemplo: 15.5 para 15.5%
     * @param razon Motivo del descuento (ej: "Cliente frecuente", "Descuento especial"). Puede estar vacío.
     * @param usuarioId ID del usuario que aplicó el descuento (auditoría)
     * @param fechaAplicacion Momento en que se aplicó el descuento
     * @throws IllegalArgumentException si el porcentaje está fuera del rango 0-100
     * @throws IllegalArgumentException si la razón es nula
     * @throws IllegalArgumentException si usuarioId o fechaAplicacion son nulos
     */
    public DescuentoManual(
            BigDecimal porcentaje, 
            String razon, 
            UUID usuarioId, 
            LocalDateTime fechaAplicacion
    ) {
        // Validar porcentaje
        Objects.requireNonNull(porcentaje, "El porcentaje no puede ser null");
        if (porcentaje.compareTo(PORCENTAJE_MINIMO) < 0 || porcentaje.compareTo(PORCENTAJE_MAXIMO) > 0) {
            throw new IllegalArgumentException(
                String.format("El porcentaje debe estar entre 0 y 100. Recibido: %s", porcentaje)
            );
        }

        // Validar razón (puede estar vacía, no puede ser null)
        Objects.requireNonNull(razon, "La razón del descuento no puede ser null");

        // Validar auditoría
        Objects.requireNonNull(usuarioId, "El usuarioId no puede ser null");
        Objects.requireNonNull(fechaAplicacion, "La fechaAplicacion no puede ser null");

        this.porcentaje = porcentaje;
        this.razon = razon;  // Se permite vacío, solo se valida que no sea null
        this.usuarioId = usuarioId;
        this.fechaAplicacion = fechaAplicacion;
    }

    /**
     * Calcula el monto del descuento sobre una base gravable.
     * 
     * Este método centraliza la lógica de cálculo y garantiza que el dinamismo funcione.
     * 
     * Fórmula: monto = base * (porcentaje / 100)
     * Ejemplo: base=$1000, porcentaje=10 → monto=$100
     * 
     * @param baseGravable Base sobre la cual calcular el descuento (puede ser subtotal de ítem o total de pedido)
     * @return Monto del descuento en valor absoluto (siempre >= 0)
     * @throws IllegalArgumentException si la base es negativa
     */
    public BigDecimal calcularMonto(BigDecimal baseGravable) {
        Objects.requireNonNull(baseGravable, "La base gravable no puede ser null");
        
        if (baseGravable.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                String.format("La base gravable no puede ser negativa: %s", baseGravable)
            );
        }

        // Fórmula: monto = base * (porcentaje / 100)
        // Redondeo: HALF_UP para evitar pérdida de centavos
        return baseGravable
                .multiply(porcentaje)
                .divide(CIEN, 2, RoundingMode.HALF_UP);
    }

    /**
     * Retorna el porcentaje del descuento.
     * 
     * @return Porcentaje (0-100)
     */
    public BigDecimal getPorcentaje() {
        return porcentaje;
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
        return Objects.equals(porcentaje, that.porcentaje) &&
               Objects.equals(razon, that.razon) &&
               Objects.equals(usuarioId, that.usuarioId) &&
               Objects.equals(fechaAplicacion, that.fechaAplicacion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(porcentaje, razon, usuarioId, fechaAplicacion);
    }

    @Override
    public String toString() {
        return String.format(
            "DescuentoManual{porcentaje=%s%%, razon='%s', usuarioId=%s, fecha=%s}",
            porcentaje, razon, usuarioId, fechaAplicacion
        );
    }
}
