package com.agustinpalma.comandas.infrastructure.persistence.entity;

import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoJornada;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA para JornadaCaja.
 * Representa la tabla jornadas_caja en la base de datos.
 * 
 * Cada registro es un snapshot contable de un día operativo cerrado.
 * La combinación (local_id, fecha_operativa) es única.
 */
@Entity
@Table(name = "jornadas_caja", 
    uniqueConstraints = @UniqueConstraint(
        name = "uk_jornada_local_fecha", 
        columnNames = {"local_id", "fecha_operativa"}
    ),
    indexes = {
        @Index(name = "idx_jornada_local_fecha", columnList = "local_id, fecha_operativa")
    }
)
public class JornadaCajaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "local_id", nullable = false)
    private UUID localId;

    @Column(name = "fecha_operativa", nullable = false)
    private LocalDate fechaOperativa;

    @Column(name = "fecha_cierre", nullable = false)
    private LocalDateTime fechaCierre;

    @Column(name = "total_ventas_reales", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalVentasReales;

    @Column(name = "total_consumo_interno", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalConsumoInterno;

    @Column(name = "total_egresos", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalEgresos;

    @Column(name = "balance_efectivo", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceEfectivo;

    @Column(name = "pedidos_cerrados_count", nullable = false)
    private int pedidosCerradosCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoJornada estado;

    // Constructor vacío requerido por JPA
    protected JornadaCajaEntity() {
    }

    public JornadaCajaEntity(UUID id, UUID localId, LocalDate fechaOperativa,
                              LocalDateTime fechaCierre, BigDecimal totalVentasReales,
                              BigDecimal totalConsumoInterno, BigDecimal totalEgresos,
                              BigDecimal balanceEfectivo, int pedidosCerradosCount,
                              EstadoJornada estado) {
        this.id = id;
        this.localId = localId;
        this.fechaOperativa = fechaOperativa;
        this.fechaCierre = fechaCierre;
        this.totalVentasReales = totalVentasReales;
        this.totalConsumoInterno = totalConsumoInterno;
        this.totalEgresos = totalEgresos;
        this.balanceEfectivo = balanceEfectivo;
        this.pedidosCerradosCount = pedidosCerradosCount;
        this.estado = estado;
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public UUID getLocalId() {
        return localId;
    }

    public LocalDate getFechaOperativa() {
        return fechaOperativa;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public BigDecimal getTotalVentasReales() {
        return totalVentasReales;
    }

    public BigDecimal getTotalConsumoInterno() {
        return totalConsumoInterno;
    }

    public BigDecimal getTotalEgresos() {
        return totalEgresos;
    }

    public BigDecimal getBalanceEfectivo() {
        return balanceEfectivo;
    }

    public int getPedidosCerradosCount() {
        return pedidosCerradosCount;
    }

    public EstadoJornada getEstado() {
        return estado;
    }
}
