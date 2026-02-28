package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago;
import com.agustinpalma.comandas.domain.model.ItemPedido;
import com.agustinpalma.comandas.domain.model.Pago;
import com.agustinpalma.comandas.domain.model.Pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta con el detalle completo de un pedido cerrado.
 * 
 * Usado por el modal de corrección de caja para mostrar ítems y pagos
 * editables sin necesidad de reabrir la mesa.
 * 
 * Se usa tanto para la consulta inicial (GET) como para la respuesta
 * tras una corrección exitosa (PUT).
 */
public record DetallePedidoCerradoResponse(
    String pedidoId,
    int numeroPedido,
    int mesaNumero,
    LocalDateTime fechaCierre,
    List<ItemDetalle> items,
    List<PagoDetalle> pagos,
    BigDecimal montoSubtotal,
    BigDecimal montoDescuentos,
    BigDecimal montoTotal
) {

    /**
     * Detalle de un ítem del pedido para edición en el modal de corrección.
     * 
     * @param itemId UUID del ítem (para identificar correcciones)
     * @param nombreProducto nombre del producto (snapshot histórico)
     * @param cantidad cantidad actual
     * @param precioUnitario precio unitario (snapshot histórico, no editable)
     * @param subtotalLinea subtotal bruto de la línea (precio × cantidad + extras, SIN descuentos)
     * @param observacion observación del cliente (ej: "sin cebolla")
     * @param montoDescuento monto total de descuentos aplicados a esta línea (promo + manual)
     * @param descripcionDescuento descripción legible del descuento (ej: "2x1 Pizza", "Desc. 10%")
     */
    public record ItemDetalle(
        String itemId,
        String nombreProducto,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotalLinea,
        String observacion,
        BigDecimal montoDescuento,
        String descripcionDescuento
    ) {
        public static ItemDetalle fromDomain(ItemPedido item) {
            BigDecimal bruto = item.calcularSubtotalLinea();
            BigDecimal neto = item.calcularPrecioFinal();
            BigDecimal descuento = bruto.subtract(neto).max(BigDecimal.ZERO);
            
            return new ItemDetalle(
                item.getId().getValue().toString(),
                item.getNombreProducto(),
                item.getCantidad(),
                item.getPrecioUnitario(),
                bruto,
                item.getObservacion(),
                descuento,
                buildDescripcionDescuento(item)
            );
        }
        
        /**
         * Construye una descripción legible del descuento aplicado al ítem.
         * Combina nombre de promoción automática + descuento manual si ambos existen.
         */
        private static String buildDescripcionDescuento(ItemPedido item) {
            StringBuilder sb = new StringBuilder();
            
            // Promoción automática (HU-10)
            if (item.getNombrePromocion() != null && !item.getNombrePromocion().isBlank()) {
                sb.append(item.getNombrePromocion());
            }
            
            // Descuento manual (HU-14)
            if (item.getDescuentoManual() != null) {
                if (sb.length() > 0) sb.append(" + ");
                sb.append("Desc. manual");
                if (item.getDescuentoManual().getRazon() != null) {
                    sb.append(": ").append(item.getDescuentoManual().getRazon());
                }
            }
            
            return sb.length() > 0 ? sb.toString() : null;
        }
    }

    /**
     * Detalle de un pago para edición en el modal de corrección.
     */
    public record PagoDetalle(
        MedioPago medio,
        BigDecimal monto
    ) {
        public static PagoDetalle fromDomain(Pago pago) {
            return new PagoDetalle(pago.getMedio(), pago.getMonto());
        }
    }

    /**
     * Factory method para crear la respuesta desde el dominio.
     * 
     * @param pedido pedido cerrado con ítems y pagos cargados
     * @param mesaNumero número de mesa resuelto desde el repositorio
     * @return DTO completo para el frontend
     */
    public static DetallePedidoCerradoResponse fromDomain(Pedido pedido, int mesaNumero) {
        return new DetallePedidoCerradoResponse(
            pedido.getId().getValue().toString(),
            pedido.getNumero(),
            mesaNumero,
            pedido.getFechaCierre(),
            pedido.getItems().stream().map(ItemDetalle::fromDomain).toList(),
            pedido.getPagos().stream().map(PagoDetalle::fromDomain).toList(),
            pedido.getMontoSubtotalFinal(),
            pedido.getMontoDescuentosFinal(),
            pedido.getMontoTotalFinal()
        );
    }
}
