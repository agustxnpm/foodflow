package com.agustinpalma.comandas.infrastructure.persistence.entity;

import com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA para Pago.
 * Representa la tabla pedidos_pagos en la base de datos.
 * 
 * Un pedido puede tener múltiples pagos (soporte split/pagos parciales).
 * La suma de todos los pagos debe coincidir con el montoTotalFinal del pedido.
 */
@Entity
@Table(name = "pedidos_pagos", indexes = {
    @Index(name = "idx_pago_pedido_id", columnList = "pedido_id")
})
public class PagoEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Relación ManyToOne con PedidoEntity.
     * Cada pago pertenece a un pedido.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoEntity pedido;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false, length = 20)
    private MedioPago medioPago;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    // Constructor vacío requerido por JPA
    protected PagoEntity() {
    }

    public PagoEntity(UUID id, MedioPago medioPago, BigDecimal monto, LocalDateTime fecha) {
        this.id = id;
        this.medioPago = medioPago;
        this.monto = monto;
        this.fecha = fecha;
    }

    // Getters y setters

    public UUID getId() {
        return id;
    }

    public PedidoEntity getPedido() {
        return pedido;
    }

    public void setPedido(PedidoEntity pedido) {
        this.pedido = pedido;
    }

    public MedioPago getMedioPago() {
        return medioPago;
    }

    public void setMedioPago(MedioPago medioPago) {
        this.medioPago = medioPago;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
