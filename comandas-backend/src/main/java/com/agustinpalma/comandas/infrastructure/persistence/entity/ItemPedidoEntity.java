package com.agustinpalma.comandas.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA para ItemPedido.
 * Representa la tabla items_pedido en la base de datos.
 * NO es la entidad de dominio - vive exclusivamente en la capa de infraestructura.
 * 
 * HU-07: Incluye relación @ManyToOne con PedidoEntity para persistencia bidireccional.
 * HU-10: Incluye campos de snapshot de promoción para auditoría y visualización.
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

    // ============================================
    // HU-10: Campos de snapshot de promoción
    // ============================================

    /**
     * Monto del descuento calculado al momento de agregar el ítem.
     * Valor monetario (ej: $250). Por defecto 0 si no hay promoción.
     */
    @Column(name = "monto_descuento", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoDescuento = BigDecimal.ZERO;

    /**
     * Nombre de la promoción para mostrar al cliente en el ticket.
     * Null si no hay promoción aplicada.
     */
    @Column(name = "nombre_promocion", length = 150)
    private String nombrePromocion;

    /**
     * ID de la promoción para auditoría y trazabilidad.
     * Null si no hay promoción aplicada.
     */
    @Column(name = "promocion_id")
    private UUID promocionId;

    // ============================================
    // HU-14: Campos de descuento manual dinámico
    // ============================================

    /**
     * Porcentaje del descuento manual aplicado al ítem.
     * Null si no tiene descuento manual.
     */
    @Column(name = "desc_manual_porcentaje", precision = 5, scale = 2)
    private BigDecimal descManualPorcentaje;

    /**
     * Razón del descuento manual.
     * Null si no tiene descuento manual.
     */
    @Column(name = "desc_manual_razon", length = 255)
    private String descManualRazon;

    /**
     * ID del usuario que aplicó el descuento manual.
     * Null si no tiene descuento manual.
     */
    @Column(name = "desc_manual_usuario_id")
    private UUID descManualUsuarioId;

    /**
     * Fecha de aplicación del descuento manual.
     * Null si no tiene descuento manual.
     */
    @Column(name = "desc_manual_fecha")
    private LocalDateTime descManualFecha;

    // Constructor vacío para JPA
    protected ItemPedidoEntity() {}

    // Constructor con parámetros (sin promoción - compatibilidad)
    public ItemPedidoEntity(UUID id, UUID productoId, String nombreProducto,
                             int cantidad, BigDecimal precioUnitario, String observacion) {
        this(id, productoId, nombreProducto, cantidad, precioUnitario, observacion, 
             BigDecimal.ZERO, null, null, null, null, null, null);
    }

    // Constructor completo con promoción (HU-10) y descuento manual (HU-14)
    public ItemPedidoEntity(
            UUID id, 
            UUID productoId, 
            String nombreProducto,
            int cantidad, 
            BigDecimal precioUnitario, 
            String observacion,
            BigDecimal montoDescuento,
            String nombrePromocion,
            UUID promocionId,
            BigDecimal descManualPorcentaje,
            String descManualRazon,
            UUID descManualUsuarioId,
            LocalDateTime descManualFecha
    ) {
        this.id = id;
        this.productoId = productoId;
        this.nombreProducto = nombreProducto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.observacion = observacion;
        this.montoDescuento = montoDescuento != null ? montoDescuento : BigDecimal.ZERO;
        this.nombrePromocion = nombrePromocion;
        this.promocionId = promocionId;
        this.descManualPorcentaje = descManualPorcentaje;
        this.descManualRazon = descManualRazon;
        this.descManualUsuarioId = descManualUsuarioId;
        this.descManualFecha = descManualFecha;
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

    // ============================================
    // HU-10: Getters y Setters de promoción
    // ============================================

    public BigDecimal getMontoDescuento() {
        return montoDescuento;
    }

    public void setMontoDescuento(BigDecimal montoDescuento) {
        this.montoDescuento = montoDescuento;
    }

    public String getNombrePromocion() {
        return nombrePromocion;
    }

    public void setNombrePromocion(String nombrePromocion) {
        this.nombrePromocion = nombrePromocion;
    }

    public UUID getPromocionId() {
        return promocionId;
    }

    public void setPromocionId(UUID promocionId) {
        this.promocionId = promocionId;
    }

    // ============================================
    // HU-14: Getters y Setters de descuento manual
    // ============================================

    public BigDecimal getDescManualPorcentaje() {
        return descManualPorcentaje;
    }

    public void setDescManualPorcentaje(BigDecimal descManualPorcentaje) {
        this.descManualPorcentaje = descManualPorcentaje;
    }

    public String getDescManualRazon() {
        return descManualRazon;
    }

    public void setDescManualRazon(String descManualRazon) {
        this.descManualRazon = descManualRazon;
    }

    public UUID getDescManualUsuarioId() {
        return descManualUsuarioId;
    }

    public void setDescManualUsuarioId(UUID descManualUsuarioId) {
        this.descManualUsuarioId = descManualUsuarioId;
    }

    public LocalDateTime getDescManualFecha() {
        return descManualFecha;
    }

    public void setDescManualFecha(LocalDateTime descManualFecha) {
        this.descManualFecha = descManualFecha;
    }
}
