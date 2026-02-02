package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate Root del contexto de Pedidos.
 * Representa una comanda/pedido asociado a una mesa.
 */
public class Pedido {

    private final PedidoId id;
    private final LocalId localId;
    private final MesaId mesaId;
    private EstadoPedido estado;
    private final LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private MedioPago medioPago;
    private final List<ItemPedido> items;
    private final List<DescuentoAplicado> descuentos;

    public Pedido(PedidoId id, LocalId localId, MesaId mesaId, EstadoPedido estado, LocalDateTime fechaApertura) {
        this.id = Objects.requireNonNull(id, "El id del pedido no puede ser null");
        this.localId = Objects.requireNonNull(localId, "El localId no puede ser null");
        this.mesaId = Objects.requireNonNull(mesaId, "El mesaId no puede ser null");
        this.estado = Objects.requireNonNull(estado, "El estado del pedido no puede ser null");
        this.fechaApertura = Objects.requireNonNull(fechaApertura, "La fecha de apertura no puede ser null");
        this.items = new ArrayList<>();
        this.descuentos = new ArrayList<>();
    }

    public PedidoId getId() {
        return id;
    }

    public LocalId getLocalId() {
        return localId;
    }

    public MesaId getMesaId() {
        return mesaId;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public LocalDateTime getFechaApertura() {
        return fechaApertura;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public MedioPago getMedioPago() {
        return medioPago;
    }

    public List<ItemPedido> getItems() {
        return Collections.unmodifiableList(items);
    }

    public List<DescuentoAplicado> getDescuentos() {
        return Collections.unmodifiableList(descuentos);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pedido pedido = (Pedido) o;
        return Objects.equals(id, pedido.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
