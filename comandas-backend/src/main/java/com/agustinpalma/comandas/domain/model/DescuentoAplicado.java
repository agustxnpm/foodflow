package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad que representa un descuento aplicado a un pedido.
 * Puede aplicarse a un ítem específico o al total del pedido.
 * Pertenece al aggregate Pedido.
 */
public class DescuentoAplicado {

    private final DescuentoId id;
    private final PedidoId pedidoId;
    private final TipoDescuento tipo;
    private final AmbitoDescuento ambito;
    private final BigDecimal porcentaje;
    private final BigDecimal monto;
    private final LocalDateTime fechaAplicacion;
    private final PromocionId promocionId; // Opcional: origen si viene de una promoción
    private final ItemPedidoId itemPedidoId; // Opcional: si aplica a un ítem específico

    public DescuentoAplicado(
            DescuentoId id,
            PedidoId pedidoId,
            TipoDescuento tipo,
            AmbitoDescuento ambito,
            BigDecimal porcentaje,
            BigDecimal monto,
            LocalDateTime fechaAplicacion,
            PromocionId promocionId,
            ItemPedidoId itemPedidoId
    ) {
        this.id = Objects.requireNonNull(id, "El id del descuento no puede ser null");
        this.pedidoId = Objects.requireNonNull(pedidoId, "El pedidoId no puede ser null");
        this.tipo = Objects.requireNonNull(tipo, "El tipo de descuento no puede ser null");
        this.ambito = Objects.requireNonNull(ambito, "El ámbito del descuento no puede ser null");
        this.porcentaje = validarPorcentaje(porcentaje);
        this.monto = validarMonto(monto);
        this.fechaAplicacion = Objects.requireNonNull(fechaAplicacion, "La fecha de aplicación no puede ser null");
        this.promocionId = promocionId; // Puede ser null
        this.itemPedidoId = itemPedidoId; // Puede ser null
    }

    private BigDecimal validarPorcentaje(BigDecimal porcentaje) {
        if (porcentaje == null) {
            throw new IllegalArgumentException("El porcentaje no puede ser null");
        }
        if (porcentaje.compareTo(BigDecimal.ZERO) < 0 || porcentaje.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100");
        }
        return porcentaje;
    }

    private BigDecimal validarMonto(BigDecimal monto) {
        if (monto == null) {
            throw new IllegalArgumentException("El monto no puede ser null");
        }
        if (monto.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo");
        }
        return monto;
    }

    public DescuentoId getId() {
        return id;
    }

    public PedidoId getPedidoId() {
        return pedidoId;
    }

    public TipoDescuento getTipo() {
        return tipo;
    }

    public AmbitoDescuento getAmbito() {
        return ambito;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public LocalDateTime getFechaAplicacion() {
        return fechaAplicacion;
    }

    public PromocionId getPromocionId() {
        return promocionId;
    }

    public ItemPedidoId getItemPedidoId() {
        return itemPedidoId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DescuentoAplicado that = (DescuentoAplicado) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
