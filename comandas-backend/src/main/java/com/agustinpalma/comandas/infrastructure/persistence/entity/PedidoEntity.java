package com.agustinpalma.comandas.infrastructure.persistence.entity;

import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPedido;
import com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad JPA para Pedido.
 * Representa la tabla de base de datos, NO el modelo de dominio.
 * Las anotaciones de JPA viven aquí, no en el dominio.
 * 
 * HU-07: Incluye relación @OneToMany con ItemPedidoEntity
 * usando cascade y orphanRemoval para persistencia atómica.
 */
@Entity
@Table(name = "pedidos", indexes = {
    @Index(name = "idx_pedido_mesa_estado", columnList = "mesa_id, estado"),
    @Index(name = "idx_pedido_local_numero", columnList = "local_id, numero")
})
public class PedidoEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "local_id", nullable = false)
    private UUID localId;

    @Column(name = "mesa_id", nullable = false)
    private UUID mesaId;

    @Column(name = "numero", nullable = false)
    private int numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPedido estado;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", length = 20)
    private MedioPago medioPago;

    // ============================================
    // HU-14: Campos de descuento global dinámico
    // ============================================

    /**
     * Porcentaje del descuento global aplicado al pedido.
     * Null si no tiene descuento global.
     */
    @Column(name = "desc_global_porcentaje", precision = 5, scale = 2)
    private BigDecimal descGlobalPorcentaje;

    /**
     * Razón del descuento global.
     * Null si no tiene descuento global.
     */
    @Column(name = "desc_global_razon", length = 255)
    private String descGlobalRazon;

    /**
     * ID del usuario que aplicó el descuento global.
     * Null si no tiene descuento global.
     */
    @Column(name = "desc_global_usuario_id")
    private UUID descGlobalUsuarioId;

    /**
     * Fecha de aplicación del descuento global.
     * Null si no tiene descuento global.
     */
    @Column(name = "desc_global_fecha")
    private LocalDateTime descGlobalFecha;

    /**
     * HU-07: Relación bidireccional con ítems del pedido.
     * 
     * CASCADE.ALL: Al guardar el pedido, automáticamente se guardan/actualizan sus ítems.
     * orphanRemoval = true: Si un ítem se elimina de la lista, se borra de la BD.
     * 
     * Esta configuración garantiza atomicidad en las operaciones de persistencia.
     */
    @OneToMany(
        mappedBy = "pedido",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private List<ItemPedidoEntity> items = new ArrayList<>();

    // ============================================
    // Pagos del pedido (soporte split / pagos parciales)
    // ============================================

    @OneToMany(
        mappedBy = "pedido",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private List<PagoEntity> pagos = new ArrayList<>();

    // ============================================
    // Snapshot contable: se congela al cerrar el pedido
    // ============================================

    @Column(name = "monto_subtotal_final", precision = 10, scale = 2)
    private BigDecimal montoSubtotalFinal;

    @Column(name = "monto_descuentos_final", precision = 10, scale = 2)
    private BigDecimal montoDescuentosFinal;

    @Column(name = "monto_total_final", precision = 10, scale = 2)
    private BigDecimal montoTotalFinal;

    // Constructor vacío requerido por JPA
    protected PedidoEntity() {
    }

    public PedidoEntity(UUID id, UUID localId, UUID mesaId, int numero, EstadoPedido estado, LocalDateTime fechaApertura) {
        this.id = id;
        this.localId = localId;
        this.mesaId = mesaId;
        this.numero = numero;
        this.estado = estado;
        this.fechaApertura = fechaApertura;
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

    public UUID getMesaId() {
        return mesaId;
    }

    public void setMesaId(UUID mesaId) {
        this.mesaId = mesaId;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(LocalDateTime fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public MedioPago getMedioPago() {
        return medioPago;
    }

    public void setMedioPago(MedioPago medioPago) {
        this.medioPago = medioPago;
    }

    // ============================================
    // HU-14: Getters y Setters de descuento global
    // ============================================

    public BigDecimal getDescGlobalPorcentaje() {
        return descGlobalPorcentaje;
    }

    public void setDescGlobalPorcentaje(BigDecimal descGlobalPorcentaje) {
        this.descGlobalPorcentaje = descGlobalPorcentaje;
    }

    public String getDescGlobalRazon() {
        return descGlobalRazon;
    }

    public void setDescGlobalRazon(String descGlobalRazon) {
        this.descGlobalRazon = descGlobalRazon;
    }

    public UUID getDescGlobalUsuarioId() {
        return descGlobalUsuarioId;
    }

    public void setDescGlobalUsuarioId(UUID descGlobalUsuarioId) {
        this.descGlobalUsuarioId = descGlobalUsuarioId;
    }

    public LocalDateTime getDescGlobalFecha() {
        return descGlobalFecha;
    }

    public void setDescGlobalFecha(LocalDateTime descGlobalFecha) {
        this.descGlobalFecha = descGlobalFecha;
    }

    public List<ItemPedidoEntity> getItems() {
        return items;
    }

    // ============================================
    // Getters y Setters de pagos
    // ============================================

    public List<PagoEntity> getPagos() {
        return pagos;
    }

    /**
     * Método helper para agregar un pago manteniendo la sincronización bidireccional.
     * 
     * @param pago el pago a agregar
     */
    public void agregarPago(PagoEntity pago) {
        pagos.add(pago);
        pago.setPedido(this);
    }

    // ============================================
    // Getters y Setters de snapshot contable
    // ============================================

    public BigDecimal getMontoSubtotalFinal() {
        return montoSubtotalFinal;
    }

    public void setMontoSubtotalFinal(BigDecimal montoSubtotalFinal) {
        this.montoSubtotalFinal = montoSubtotalFinal;
    }

    public BigDecimal getMontoDescuentosFinal() {
        return montoDescuentosFinal;
    }

    public void setMontoDescuentosFinal(BigDecimal montoDescuentosFinal) {
        this.montoDescuentosFinal = montoDescuentosFinal;
    }

    public BigDecimal getMontoTotalFinal() {
        return montoTotalFinal;
    }

    public void setMontoTotalFinal(BigDecimal montoTotalFinal) {
        this.montoTotalFinal = montoTotalFinal;
    }

    /**
     * HU-07: Método helper para agregar un ítem manteniendo la sincronización bidireccional.
     * Establece automáticamente la referencia inversa del ítem al pedido.
     * 
     * @param item el ítem a agregar
     */
    public void agregarItem(ItemPedidoEntity item) {
        items.add(item);
        item.setPedido(this);
    }

    /**
     * HU-07: Método helper para eliminar un ítem manteniendo la sincronización bidireccional.
     * Limpia la referencia inversa del ítem al pedido.
     * 
     * @param item el ítem a eliminar
     */
    public void eliminarItem(ItemPedidoEntity item) {
        items.remove(item);
        item.setPedido(null);
    }
}
