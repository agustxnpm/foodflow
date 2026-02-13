package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainEnums.TipoMovimiento;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MovimientoCajaId;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Aggregate Root del contexto de Caja.
 * Representa un movimiento de caja (egreso de efectivo).
 * 
 * Un MovimientoCaja registra una salida de dinero del local,
 * por ejemplo: compras de insumos de limpieza, reparaciones, etc.
 * 
 * Reglas de negocio:
 * - El monto siempre es positivo (representa el valor absoluto del egreso)
 * - Al crearse genera automáticamente un número de comprobante único
 * - El comprobante tiene formato "EGR-yyyyMMdd-HHmmss-XXXX" donde XXXX son los últimos 4 chars del ID
 * - Es inmutable después de la creación
 */
public class MovimientoCaja {

    private static final DateTimeFormatter FORMATO_COMPROBANTE = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final MovimientoCajaId id;
    private final LocalId localId;
    private final BigDecimal monto;
    private final String descripcion;
    private final LocalDateTime fecha;
    private final TipoMovimiento tipo;
    private final String numeroComprobante;

    /**
     * Crea un nuevo movimiento de caja con generación automática de comprobante.
     * 
     * @param id identificador único del movimiento
     * @param localId identificador del local (tenant)
     * @param monto monto del egreso (debe ser > 0)
     * @param descripcion descripción del egreso (ej: "Productos de limpieza")
     * @param fecha fecha y hora del movimiento
     * @throws NullPointerException si algún argumento obligatorio es null
     * @throws IllegalArgumentException si el monto es <= 0 o la descripción está vacía
     */
    public MovimientoCaja(MovimientoCajaId id, LocalId localId, BigDecimal monto, 
                          String descripcion, LocalDateTime fecha) {
        this.id = Objects.requireNonNull(id, "El id del movimiento no puede ser null");
        this.localId = Objects.requireNonNull(localId, "El localId no puede ser null");
        this.monto = validarMonto(Objects.requireNonNull(monto, "El monto no puede ser null"));
        this.descripcion = validarDescripcion(descripcion);
        this.fecha = Objects.requireNonNull(fecha, "La fecha no puede ser null");
        this.tipo = TipoMovimiento.EGRESO;
        this.numeroComprobante = generarNumeroComprobante(id, fecha);
    }

    /**
     * Constructor para reconstrucción desde persistencia.
     * Permite especificar el número de comprobante ya generado.
     */
    public MovimientoCaja(MovimientoCajaId id, LocalId localId, BigDecimal monto,
                          String descripcion, LocalDateTime fecha, TipoMovimiento tipo,
                          String numeroComprobante) {
        this.id = Objects.requireNonNull(id, "El id del movimiento no puede ser null");
        this.localId = Objects.requireNonNull(localId, "El localId no puede ser null");
        this.monto = Objects.requireNonNull(monto, "El monto no puede ser null");
        this.descripcion = Objects.requireNonNull(descripcion, "La descripción no puede ser null");
        this.fecha = Objects.requireNonNull(fecha, "La fecha no puede ser null");
        this.tipo = Objects.requireNonNull(tipo, "El tipo no puede ser null");
        this.numeroComprobante = Objects.requireNonNull(numeroComprobante, "El número de comprobante no puede ser null");
    }

    private BigDecimal validarMonto(BigDecimal monto) {
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                String.format("El monto del egreso debe ser mayor a cero. Recibido: %s", monto)
            );
        }
        return monto;
    }

    private String validarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripción del egreso no puede estar vacía");
        }
        return descripcion.trim();
    }

    /**
     * Genera un número de comprobante único con formato legible.
     * Formato: EGR-yyyyMMdd-HHmmss-XXXX
     * 
     * @param id identificador del movimiento
     * @param fecha fecha del movimiento
     * @return número de comprobante generado
     */
    private String generarNumeroComprobante(MovimientoCajaId id, LocalDateTime fecha) {
        String sufijo = id.getValue().toString().substring(0, 4).toUpperCase();
        return String.format("EGR-%s-%s", fecha.format(FORMATO_COMPROBANTE), sufijo);
    }

    // ============================================
    // Getters (inmutable, sin setters)
    // ============================================

    public MovimientoCajaId getId() {
        return id;
    }

    public LocalId getLocalId() {
        return localId;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public String getNumeroComprobante() {
        return numeroComprobante;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MovimientoCaja that = (MovimientoCaja) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("MovimientoCaja{id=%s, tipo=%s, monto=%s, comprobante=%s}",
            id, tipo, monto, numeroComprobante);
    }
}
