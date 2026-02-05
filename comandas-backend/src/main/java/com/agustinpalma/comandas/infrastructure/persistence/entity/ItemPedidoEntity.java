package com.agustinpalma.comandas.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidad JPA para ItemPedido.
 * Representa la tabla items_pedido en la base de datos.
 * NO es la entidad de dominio - vive exclusivamente en la capa de infraestructura.
 * 
 * HU-07: Incluye relación @ManyToOne con PedidoEntity para persistencia bidireccional.
 */
@Entity
@Table(name = "items_pedido", indexes = {
    @Index(name = "idx_item_pedido_id", columnList = "pedido_id")
})
public class ItemPedidoEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * HU-07: Relación ManyToOne con PedidoEntity.
     * Esta es la parte "muchos" de la relación bidireccional.
     * El campo pedido_id se mapea automáticamente desde esta relación.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoEntity pedido;

    @Column(name = "producto_id", nullable = false)
    private UUID productoId;

    @Column(name = "nombre_producto", nullable = false, length = 100)
    private String nombreProducto;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "observacion", length = 255)
    private String observacion;

    // Constructor vacío para JPA
    protected ItemPedidoEntity() {}

    // Constructor con parámetros
    public ItemPedidoEntity(UUID id, UUID productoId, String nombreProducto,
                             int cantidad, BigDecimal precioUnitario, String observacion) {
        this.id = id;
        this.productoId = productoId;
        this.nombreProducto = nombreProducto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.observacion = observacion;
    }

    // Getters y setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PedidoEntity getPedido() {
        return pedido;
    }

    public void setPedido(PedidoEntity pedido) {
        this.pedido = pedido;
    }

    /**
     * Helper para obtener el ID del pedido desde la relación.
     * Útil para compatibilidad con código existente.
     */
    public UUID getPedidoId() {
        return pedido != null ? pedido.getId() : null;
    }

    public UUID getProductoId() {
        return productoId;
    }

    public void setProductoId(UUID productoId) {
        this.productoId = productoId;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
