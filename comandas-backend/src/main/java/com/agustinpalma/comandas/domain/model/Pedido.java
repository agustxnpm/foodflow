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


    /**
     * Finaliza el pedido registrando el medio de pago y la fecha de cierre.
     * Transiciona el estado de ABIERTO a CERRADO.
     *
     * Reglas de negocio:
     * - Solo se pueden cerrar pedidos en estado ABIERTO
     * - El pedido debe tener al menos un ítem cargado
     * - El medio de pago es obligatorio
     * - La fecha de cierre queda registrada para auditoría
     *
     * @param medio el medio de pago utilizado (obligatorio)
     * @param fechaCierre la fecha y hora del cierre (obligatorio)
     * @throws IllegalStateException si el pedido no está ABIERTO
     * @throws IllegalArgumentException si el pedido no tiene ítems o el medio de pago es nulo
     */
    public void finalizar(MedioPago medio, LocalDateTime fechaCierre) {
        Objects.requireNonNull(medio, "El medio de pago es obligatorio para cerrar el pedido");
        Objects.requireNonNull(fechaCierre, "La fecha de cierre es obligatoria");

        if (this.estado != EstadoPedido.ABIERTO) {
            throw new IllegalStateException("Solo se pueden cerrar pedidos que estén en estado ABIERTO");
        }

        if (this.items.isEmpty()) {
            throw new IllegalArgumentException("No se puede cerrar un pedido sin ítems");
        }

        this.medioPago = medio;
        this.fechaCierre = fechaCierre;
        this.estado = EstadoPedido.CERRADO;
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
