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
     * 
     * HU-14 FIX: El DTO NO calcula matemáticas propias, confía en el Dominio.
     * El total incluye el dinamismo del descuento global.
     */
    public static AgregarProductoResponse fromDomain(Pedido pedido) {
        List<ItemPedidoDTO> itemsDTO = pedido.getItems().stream()
            .map(ItemPedidoDTO::fromDomain)
            .toList();

        BigDecimal subtotal = pedido.calcularSubtotalItems();
        BigDecimal total = pedido.calcularTotal();  // 🔑 Usa el método del dominio (incluye descuento global)
        
        // Total de descuentos = la diferencia entre bruto y neto
        // Incluye: Promos Automáticas + Descuentos Manuales Ítem + Descuento Global
        BigDecimal totalDescuentos = subtotal.subtract(total);

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
     * HU-14: descuentoTotal incluye promociones automáticas + descuentos manuales por ítem.
     */
    public record ItemPedidoDTO(
        String itemId,
        String productoId,
        String nombreProducto,
        int cantidad,
        BigDecimal precioUnitarioBase,    // El precio de lista (para tachar en UI)
        BigDecimal subtotalItem,           // precioBase * cantidad (sin descuento)
        BigDecimal descuentoTotal,         // El ahorro total del ítem (promo + manual)
        BigDecimal precioFinal,            // Lo que paga el cliente (subtotal - descuento)
        String observacion,
        String nombrePromocion,            // Para mostrar etiqueta de la promo
        boolean tienePromocion             // Flag para UI
    ) {
        public static ItemPedidoDTO fromDomain(ItemPedido item) {
            BigDecimal subtotalItem = item.calcularSubtotal();
            BigDecimal precioFinal = item.calcularPrecioFinal();
            
            // Descuento total del ítem = Promo Automática + Descuento Manual de Línea
            BigDecimal descuentoTotal = subtotalItem.subtract(precioFinal);
            
            return new ItemPedidoDTO(
                item.getId().getValue().toString(),
                item.getProductoId().getValue().toString(),
                item.getNombreProducto(),
                item.getCantidad(),
                item.getPrecioUnitario(),
                subtotalItem,
                descuentoTotal,
                precioFinal,
                item.getObservacion(),
                item.getNombrePromocion(),
                item.tienePromocion()
            );
        }
    }
}
