package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.model.Pedido;

/**
 * DTO de salida para el caso de uso AbrirMesa.
 * Contiene la información de la mesa abierta y el pedido creado.
 */
public record AbrirMesaResponse(
    String mesaId,
    int numeroMesa,
    String estadoMesa,
    String pedidoId,
    int numeroPedido,
    String estadoPedido,
    String fechaApertura
) {
    /**
     * Factory method para construir desde entidades de dominio.
     */
    public static AbrirMesaResponse fromDomain(Mesa mesa, Pedido pedido) {
        return new AbrirMesaResponse(
            mesa.getId().getValue().toString(),
            mesa.getNumero(),
            mesa.getEstado().name(),
            pedido.getId().getValue().toString(),
            pedido.getNumero(),
            pedido.getEstado().name(),
            pedido.getFechaApertura().toString()
        );
    }
}
