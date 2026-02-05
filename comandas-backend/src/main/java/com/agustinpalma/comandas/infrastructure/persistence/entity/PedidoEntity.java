package com.agustinpalma.comandas.infrastructure.persistence.entity;

import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPedido;
import com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago;
import jakarta.persistence.*;

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

    public List<ItemPedidoEntity> getItems() {
        return items;
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
