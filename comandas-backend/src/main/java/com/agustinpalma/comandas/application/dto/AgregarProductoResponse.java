package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.ItemPedido;
import com.agustinpalma.comandas.domain.model.Pedido;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de salida para el caso de uso AgregarProducto.
 * Contiene la información del pedido actualizado con los ítems agregados.
 * 
 * HU-05: Agregar productos a un pedido
 */
public record AgregarProductoResponse(
    String pedidoId,
    int numeroPedido,
    String estadoPedido,
    List<ItemPedidoDTO> items,
    BigDecimal subtotal,
    String fechaApertura
) {
    /**
     * Factory method para construir desde la entidad de dominio.
     */
    public static AgregarProductoResponse fromDomain(Pedido pedido) {
        List<ItemPedidoDTO> itemsDTO = pedido.getItems().stream()
            .map(ItemPedidoDTO::fromDomain)
            .toList();

        return new AgregarProductoResponse(
            pedido.getId().getValue().toString(),
            pedido.getNumero(),
            pedido.getEstado().name(),
            itemsDTO,
            pedido.calcularSubtotalItems(),
            pedido.getFechaApertura().toString()
        );
    }

    /**
     * DTO anidado para representar un ítem del pedido.
     */
    public record ItemPedidoDTO(
        String itemId,
        String productoId,
        String nombreProducto,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotalItem,
        String observacion
    ) {
        public static ItemPedidoDTO fromDomain(ItemPedido item) {
            return new ItemPedidoDTO(
                item.getId().getValue().toString(),
                item.getProductoId().getValue().toString(),
                item.getNombreProducto(),
                item.getCantidad(),
                item.getPrecioUnitario(),
                item.calcularSubtotal(),
                item.getObservacion()
            );
        }
    }
}
