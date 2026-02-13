package com.agustinpalma.comandas.infrastructure.persistence.entity;

import com.agustinpalma.comandas.domain.model.DomainEnums.TipoMovimiento;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA para MovimientoCaja.
 * Representa la tabla movimientos_caja en la base de datos.
 * 
 * Un movimiento de caja registra un egreso de efectivo del local.
 * Incluye un número de comprobante generado automáticamente por el dominio.
 */
@Entity
@Table(name = "movimientos_caja", indexes = {
    @Index(name = "idx_movimiento_caja_local_fecha", columnList = "local_id, fecha")
})
public class MovimientoCajaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "local_id", nullable = false)
    private UUID localId;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "descripcion", nullable = false, length = 500)
    private String descripcion;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoMovimiento tipo;

    @Column(name = "numero_comprobante", nullable = false, length = 50, unique = true)
    private String numeroComprobante;

    // Constructor vacío requerido por JPA
    protected MovimientoCajaEntity() {
    }

    public MovimientoCajaEntity(UUID id, UUID localId, BigDecimal monto, String descripcion,
                                 LocalDateTime fecha, TipoMovimiento tipo, String numeroComprobante) {
        this.id = id;
        this.localId = localId;
        this.monto = monto;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.tipo = tipo;
        this.numeroComprobante = numeroComprobante;
    }

    // Getters y setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getLocalId() {
        return localId;
    }

    public void setLocalId(UUID localId) {
        this.localId = localId;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimiento tipo) {
        this.tipo = tipo;
    }

    public String getNumeroComprobante() {
        return numeroComprobante;
    }

    public void setNumeroComprobante(String numeroComprobante) {
        this.numeroComprobante = numeroComprobante;
    }
}
