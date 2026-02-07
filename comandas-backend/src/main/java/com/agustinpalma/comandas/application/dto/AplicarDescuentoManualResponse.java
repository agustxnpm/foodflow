package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.Pedido;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO de respuesta para aplicación de descuento manual (HU-14).
 * 
 * Proporciona transparencia sobre el desglose de descuentos:
 * - Subtotal bruto (sin descuentos)
 * - Descuentos automáticos (promociones HU-10)
 * - Descuentos manuales por ítem
 * - Descuento global
 * - Total final
 * 
 * @param pedidoId ID del pedido
 * @param items Lista de ítems con sus descuentos aplicados
 * @param subtotalBruto Suma de (precioUnitario * cantidad) sin descuentos
 * @param totalPromocionesAuto Suma de descuentos de promociones (HU-10)
 * @param montoDescuentoManualItems Suma de descuentos manuales por ítem
 * @param montoDescuentoGlobal Descuento global aplicado
 * @param totalFinal Total final del pedido
 * @param tieneDescuentoGlobal Indica si hay descuento global aplicado
 */
public record AplicarDescuentoManualResponse(
    UUID pedidoId,
    List<ItemDescuentoDTO> items,
    BigDecimal subtotalBruto,
    BigDecimal totalPromocionesAuto,
    BigDecimal montoDescuentoManualItems,
    BigDecimal montoDescuentoGlobal,
    BigDecimal totalFinal,
    boolean tieneDescuentoGlobal
) {
    /**
     * Factory method para construir desde entidad de dominio.
     * 
     * @param pedido Pedido del dominio con descuentos aplicados
     * @return DTO con desglose completo
     */
    public static AplicarDescuentoManualResponse fromDomain(Pedido pedido) {
        // 1. Subtotal bruto (sin ningún descuento)
        BigDecimal subtotalBruto = pedido.calcularSubtotalItems();
        
        // 2. Total de promociones automáticas (HU-10)
        BigDecimal totalPromocionesAuto = pedido.getItems().stream()
            .map(item -> item.getMontoDescuento())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 3. Total de descuentos manuales por ítem
        BigDecimal montoDescuentoManualItems = pedido.getItems().stream()
            .map(item -> item.calcularMontoDescuentoManual())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 4. Descuento global
        BigDecimal montoDescuentoGlobal = pedido.calcularMontoDescuentoGlobal();
        
        // 5. Total final (ya calculado por el dominio)
        BigDecimal totalFinal = pedido.calcularTotal();
        
        // 6. Mapear ítems
        List<ItemDescuentoDTO> itemsDTO = pedido.getItems().stream()
            .map(item -> new ItemDescuentoDTO(
                item.getId().getValue(),
                item.getNombreProducto(),
                item.getCantidad(),
                item.getPrecioUnitario(),
                item.calcularSubtotal(),
                item.getMontoDescuento(),
                item.getNombrePromocion(),
                item.calcularMontoDescuentoManual(),
                item.tieneDescuentoManual() ? item.getDescuentoManual().getPorcentaje() : null,
                item.calcularPrecioFinal()
            ))
            .toList();
        
        return new AplicarDescuentoManualResponse(
            pedido.getId().getValue(),
            itemsDTO,
            subtotalBruto,
            totalPromocionesAuto,
            montoDescuentoManualItems,
            montoDescuentoGlobal,
            totalFinal,
            pedido.tieneDescuentoGlobal()
        );
    }

    /**
     * DTO para ítem con desglose de descuentos.
     */
    public record ItemDescuentoDTO(
        UUID itemId,
        String nombreProducto,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotalBruto,
        BigDecimal montoDescuentoPromo,
        String nombrePromocion,
        BigDecimal montoDescuentoManual,
        BigDecimal porcentajeDescuentoManual,
        BigDecimal precioFinal
    ) {}
}
