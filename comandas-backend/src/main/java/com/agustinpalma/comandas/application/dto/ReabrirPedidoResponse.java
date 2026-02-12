package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoMesa;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPedido;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.model.Pedido;

import java.time.LocalDateTime;

/**
 * DTO de salida para el caso de uso de reapertura de pedido (HU-14).
 * 
 * Incluye información de la mesa reocupada y el pedido reabierto,
 * confirmando que la operación de reversión fue exitosa.
 *
 * @param mesaId identificador de la mesa
 * @param mesaNumero número de la mesa
 * @param mesaEstado estado de la mesa (debe ser ABIERTA)
 * @param pedidoId identificador del pedido reabierto
 * @param pedidoNumero número secuencial del pedido
 * @param pedidoEstado estado del pedido (debe ser ABIERTO)
 * @param fechaReapertura timestamp de cuando se realizó la reapertura
 * @param cantidadItems cantidad de ítems que conserva el pedido
 */
public record ReabrirPedidoResponse(
    String mesaId,
    int mesaNumero,
    EstadoMesa mesaEstado,
    String pedidoId,
    int pedidoNumero,
    EstadoPedido pedidoEstado,
    LocalDateTime fechaReapertura,
    int cantidadItems
) {
    /**
     * Factory method para crear la respuesta desde las entidades de dominio.
     *
     * @param mesa la mesa que fue reocupada
     * @param pedido el pedido que fue reabierto
     * @param fechaReapertura timestamp de la operación
     * @return el DTO de respuesta
     */
    public static ReabrirPedidoResponse fromDomain(Mesa mesa, Pedido pedido, LocalDateTime fechaReapertura) {
        return new ReabrirPedidoResponse(
            mesa.getId().getValue().toString(),
            mesa.getNumero(),
            mesa.getEstado(),
            pedido.getId().getValue().toString(),
            pedido.getNumero(),
            pedido.getEstado(),
            fechaReapertura,
            pedido.getItems().size()
        );
    }
}
