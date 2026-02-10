package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value Object que representa un pago asociado a un pedido.
 * 
 * Un pedido puede tener múltiples pagos (pagos parciales/split).
 * La suma de todos los pagos debe coincidir exactamente con el total del pedido.
 * 
 * Inmutable y validado en construcción (fail fast).
 */
public final class Pago {

    private final MedioPago medio;
    private final BigDecimal monto;
    private final LocalDateTime fecha;

    /**
     * Constructor con validación estricta.
     * 
     * @param medio medio de pago utilizado (EFECTIVO, TARJETA, TRANSFERENCIA, QR)
     * @param monto monto del pago (debe ser > 0)
     * @param fecha fecha y hora del pago
     * @throws NullPointerException si algún argumento es null
     * @throws IllegalArgumentException si el monto es <= 0
     */
    public Pago(MedioPago medio, BigDecimal monto, LocalDateTime fecha) {
        this.medio = Objects.requireNonNull(medio, "El medio de pago no puede ser null");
        this.monto = validarMonto(Objects.requireNonNull(monto, "El monto no puede ser null"));
        this.fecha = Objects.requireNonNull(fecha, "La fecha del pago no puede ser null");
    }

    private BigDecimal validarMonto(BigDecimal monto) {
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                String.format("El monto del pago debe ser mayor a cero. Recibido: %s", monto)
            );
        }
        return monto;
    }

    public MedioPago getMedio() {
        return medio;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pago pago = (Pago) o;
        return medio == pago.medio &&
               monto.compareTo(pago.monto) == 0 &&
               Objects.equals(fecha, pago.fecha);
    }

    @Override
    public int hashCode() {
        return Objects.hash(medio, monto, fecha);
    }

    @Override
    public String toString() {
        return String.format("Pago{medio=%s, monto=%s, fecha=%s}", medio, monto, fecha);
    }
}
