package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.DomainIds.ItemPedidoId;
import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;

/**
 * DTO de entrada para modificar la cantidad de un ítem en un pedido.
 * 
 * HU-21: Modificar cantidad de un producto en pedido abierto.
 * 
 * La interpretación semántica de la cantidad se delega al dominio:
 * - cantidad == 0 → eliminar ítem
 * - cantidad > 0  → actualizar cantidad
 * - cantidad < 0  → rechazado por el dominio
 */
public record ModificarCantidadItemRequest(
    PedidoId pedidoId,
    ItemPedidoId itemPedidoId,
    int cantidad
) {
    public ModificarCantidadItemRequest {
        if (pedidoId == null) throw new IllegalArgumentException("El pedidoId es obligatorio");
        if (itemPedidoId == null) throw new IllegalArgumentException("El itemPedidoId es obligatorio");
    }
}
