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
 * HU-10: Incluye información de promociones aplicadas
 */
public record AgregarProductoResponse(
    String pedidoId,
    int numeroPedido,
    String estadoPedido,
    List<ItemPedidoDTO> items,
    BigDecimal subtotal,
    BigDecimal totalDescuentos,
    BigDecimal total,
    String fechaApertura
) {
    /**
     * Factory method para construir desde la entidad de dominio.
     */
    public static AgregarProductoResponse fromDomain(Pedido pedido) {
        List<ItemPedidoDTO> itemsDTO = pedido.getItems().stream()
            .map(ItemPedidoDTO::fromDomain)
            .toList();

        BigDecimal subtotal = pedido.calcularSubtotalItems();
        BigDecimal totalDescuentos = pedido.getItems().stream()
            .map(ItemPedido::getMontoDescuento)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = subtotal.subtract(totalDescuentos);

        return new AgregarProductoResponse(
            pedido.getId().getValue().toString(),
            pedido.getNumero(),
            pedido.getEstado().name(),
            itemsDTO,
            subtotal,
            totalDescuentos,
            total,
            pedido.getFechaApertura().toString()
        );
    }

    /**
     * DTO anidado para representar un ítem del pedido.
     * HU-10: Incluye campos de promoción para UX (precio tachado, ahorro, precio final).
     */
    public record ItemPedidoDTO(
        String itemId,
        String productoId,
        String nombreProducto,
        int cantidad,
        BigDecimal precioUnitarioBase,    // El precio de lista (para tachar en UI)
        BigDecimal subtotalItem,           // precioBase * cantidad (sin descuento)
        BigDecimal descuentoTotal,         // El ahorro (monto del descuento)
        BigDecimal precioFinal,            // Lo que paga el cliente (subtotal - descuento)
        String observacion,
        String nombrePromocion,            // Para mostrar etiqueta de la promo
        boolean tienePromocion             // Flag para UI
    ) {
        public static ItemPedidoDTO fromDomain(ItemPedido item) {
            return new ItemPedidoDTO(
                item.getId().getValue().toString(),
                item.getProductoId().getValue().toString(),
                item.getNombreProducto(),
                item.getCantidad(),
                item.getPrecioUnitario(),
                item.calcularSubtotal(),
                item.getMontoDescuento(),
                item.calcularPrecioFinal(),
                item.getObservacion(),
                item.getNombrePromocion(),
                item.tienePromocion()
            );
        }
    }
}
