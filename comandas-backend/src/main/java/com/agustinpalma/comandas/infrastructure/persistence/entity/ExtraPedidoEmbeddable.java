package com.agustinpalma.comandas.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Embeddable para representar extras de items de pedido.
 * Almacena el snapshot del extra en el momento de agregarse al pedido.
 */
@Embeddable
public class ExtraPedidoEmbeddable {

    @Column(name = "producto_id", nullable = false)
    private UUID productoId;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "precio_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioSnapshot;

    // Constructor vacío para JPA
    public ExtraPedidoEmbeddable() {}

    // Constructor con parámetros
    public ExtraPedidoEmbeddable(UUID productoId, String nombre, BigDecimal precioSnapshot) {
        this.productoId = productoId;
        this.nombre = nombre;
        this.precioSnapshot = precioSnapshot;
    }

    // Getters y setters
    public UUID getProductoId() {
        return productoId;
    }

    public void setProductoId(UUID productoId) {
        this.productoId = productoId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getPrecioSnapshot() {
        return precioSnapshot;
    }

    public void setPrecioSnapshot(BigDecimal precioSnapshot) {
        this.precioSnapshot = precioSnapshot;
    }
}
