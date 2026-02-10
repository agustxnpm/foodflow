package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoMesa;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPedido;
import com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.model.Pago;
import com.agustinpalma.comandas.domain.model.Pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de salida para el caso de uso de cerrar mesa.
 * Incluye información de la mesa liberada, el pedido cerrado y el snapshot contable.
 *
 * @param mesaId identificador de la mesa
 * @param mesaNumero número de la mesa
 * @param mesaEstado estado de la mesa (debe ser LIBRE)
 * @param pedidoId identificador del pedido cerrado
 * @param pedidoEstado estado del pedido (debe ser CERRADO)
 * @param montoSubtotal subtotal congelado (antes de descuentos)
 * @param montoDescuentos monto total de descuentos aplicados
 * @param montoTotal total final congelado
 * @param pagos lista de pagos registrados
 * @param fechaCierre timestamp del cierre
 */
public record CerrarMesaResponse(
    String mesaId,
    int mesaNumero,
    EstadoMesa mesaEstado,
    String pedidoId,
    EstadoPedido pedidoEstado,
    BigDecimal montoSubtotal,
    BigDecimal montoDescuentos,
    BigDecimal montoTotal,
    List<PagoResponse> pagos,
    LocalDateTime fechaCierre
) {
    /**
     * DTO anidado para representar un pago en la respuesta.
     */
    public record PagoResponse(
        MedioPago medio,
        BigDecimal monto,
        LocalDateTime fecha
    ) {
        public static PagoResponse fromDomain(Pago pago) {
            return new PagoResponse(pago.getMedio(), pago.getMonto(), pago.getFecha());
        }
    }

    /**
     * Factory method para crear la respuesta desde las entidades de dominio.
     *
     * @param mesa la mesa que fue liberada
     * @param pedido el pedido que fue cerrado
     * @return el DTO de respuesta
     */
    public static CerrarMesaResponse fromDomain(Mesa mesa, Pedido pedido) {
        List<PagoResponse> pagosResponse = pedido.getPagos().stream()
            .map(PagoResponse::fromDomain)
            .toList();

        return new CerrarMesaResponse(
            mesa.getId().getValue().toString(),
            mesa.getNumero(),
            mesa.getEstado(),
            pedido.getId().getValue().toString(),
            pedido.getEstado(),
            pedido.getMontoSubtotalFinal(),
            pedido.getMontoDescuentosFinal(),
            pedido.getMontoTotalFinal(),
            pagosResponse,
            pedido.getFechaCierre()
        );
    }
}
