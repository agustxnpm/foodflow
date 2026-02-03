package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoMesa;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPedido;
import com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.model.Pedido;

import java.time.LocalDateTime;

/**
 * DTO de salida para el caso de uso de cerrar mesa.
 * Incluye información de la mesa liberada y el pedido cerrado.
 *
 * @param mesaId identificador de la mesa
 * @param mesaNumero número de la mesa
 * @param mesaEstado estado de la mesa (debe ser LIBRE)
 * @param pedidoId identificador del pedido cerrado
 * @param pedidoEstado estado del pedido (debe ser CERRADO)
 * @param medioPago medio de pago registrado
 * @param fechaCierre timestamp del cierre
 */
public record CerrarMesaResponse(
    String mesaId,
    int mesaNumero,
    EstadoMesa mesaEstado,
    String pedidoId,
    EstadoPedido pedidoEstado,
    MedioPago medioPago,
    LocalDateTime fechaCierre
) {
    /**
     * Factory method para crear la respuesta desde las entidades de dominio.
     *
     * @param mesa la mesa que fue liberada
     * @param pedido el pedido que fue cerrado
     * @return el DTO de respuesta
     */
    public static CerrarMesaResponse fromDomain(Mesa mesa, Pedido pedido) {
        return new CerrarMesaResponse(
            mesa.getId().getValue().toString(),
            mesa.getNumero(),
            mesa.getEstado(),
            pedido.getId().getValue().toString(),
            pedido.getEstado(),
            pedido.getMedioPago(),
            pedido.getFechaCierre()
        );
    }
}
