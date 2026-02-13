package com.agustinpalma.comandas.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for stock movements (audit trail).
 * HU-22: Stock management.
 */
@Entity
@Table(name = "movimientos_stock")
public class MovimientoStockEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "producto_id", nullable = false)
    private UUID productoId;

    @Column(name = "local_id", nullable = false)
    private UUID localId;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "tipo", nullable = false, length = 30)
    private String tipo;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;

    public MovimientoStockEntity() {}

    public MovimientoStockEntity(UUID id, UUID productoId, UUID localId, int cantidad,
                                  String tipo, LocalDateTime fecha, String motivo) {
        this.id = id;
        this.productoId = productoId;
        this.localId = localId;
        this.cantidad = cantidad;
        this.tipo = tipo;
        this.fecha = fecha;
        this.motivo = motivo;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProductoId() { return productoId; }
    public void setProductoId(UUID productoId) { this.productoId = productoId; }

    public UUID getLocalId() { return localId; }
    public void setLocalId(UUID localId) { this.localId = localId; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
