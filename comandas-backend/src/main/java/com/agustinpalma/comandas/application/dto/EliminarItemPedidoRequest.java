package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.DomainIds.ItemPedidoId;
import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;

/**
 * DTO de entrada para eliminar un ítem de un pedido.
 * 
 * HU-20: Eliminar producto de un pedido abierto.
 */
public record EliminarItemPedidoRequest(
    PedidoId pedidoId,
    ItemPedidoId itemPedidoId
) {
    public EliminarItemPedidoRequest {
        if (pedidoId == null) throw new IllegalArgumentException("El pedidoId es obligatorio");
        if (itemPedidoId == null) throw new IllegalArgumentException("El itemPedidoId es obligatorio");
    }
}
