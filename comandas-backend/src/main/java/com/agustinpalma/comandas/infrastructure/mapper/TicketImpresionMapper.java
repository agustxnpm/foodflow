package com.agustinpalma.comandas.infrastructure.mapper;

import com.agustinpalma.comandas.application.dto.ComandaImpresionResponse;
import com.agustinpalma.comandas.application.dto.ComandaImpresionResponse.HeaderComanda;
import com.agustinpalma.comandas.application.dto.ComandaImpresionResponse.ItemComanda;
import com.agustinpalma.comandas.application.dto.DetallePedidoResponse;
import com.agustinpalma.comandas.application.dto.ItemDetalleDTO;
import com.agustinpalma.comandas.application.dto.TicketImpresionResponse;
import com.agustinpalma.comandas.application.dto.TicketImpresionResponse.DesglosePromocion;
import com.agustinpalma.comandas.application.dto.TicketImpresionResponse.FooterTicket;
import com.agustinpalma.comandas.application.dto.TicketImpresionResponse.HeaderTicket;
import com.agustinpalma.comandas.application.dto.TicketImpresionResponse.ItemTicket;
import com.agustinpalma.comandas.application.dto.TicketImpresionResponse.TotalesTicket;
import com.agustinpalma.comandas.infrastructure.config.MeisenProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Mapper de infraestructura para transformar DetallePedidoResponse en DTOs de impresión.
 *
 * Actúa como adaptador de presentación: toma datos ya calculados por el dominio
 * (expuestos vía DetallePedidoResponse) y los reorganiza para impresión térmica.
 *
 * No contiene lógica de negocio — solo transformación estructural.
 * Los cálculos financieros ya vienen resueltos desde el dominio.
 */
@Component
public class TicketImpresionMapper {

    private final MeisenProperties properties;

    public TicketImpresionMapper(MeisenProperties properties) {
        this.properties = Objects.requireNonNull(properties, "MeisenProperties es obligatorio");
    }

    /**
     * Transforma DetallePedidoResponse en TicketImpresionResponse (ticket de venta para cliente).
     *
     * @param detalle respuesta del caso de uso ConsultarDetallePedidoUseCase
     * @return DTO estructurado para renderizado de ticket térmico
     */
    public TicketImpresionResponse toTicket(DetallePedidoResponse detalle) {
        Objects.requireNonNull(detalle, "El detalle del pedido no puede ser nulo");

        var header = new HeaderTicket(
                properties.getLocal().getNombreLocal(),
                properties.getLocal().getDireccion(),
                properties.getLocal().getTelefono(),
                properties.getLocal().getCuit(),
                LocalDateTime.now(),
                detalle.numeroMesa()
        );

        List<ItemTicket> items = detalle.items().stream()
                .map(this::toItemTicket)
                .toList();

        var totales = calcularTotales(detalle);

        var footer = new FooterTicket(properties.getLocal().getMensajeBienvenida());

        return new TicketImpresionResponse(header, items, totales, footer);
    }

    /**
     * Transforma DetallePedidoResponse en ComandaImpresionResponse (comanda operativa para cocina/barra).
     *
     * @param detalle respuesta del caso de uso ConsultarDetallePedidoUseCase
     * @return DTO estructurado para renderizado de comanda térmica (sin precios)
     */
    public ComandaImpresionResponse toComanda(DetallePedidoResponse detalle) {
        Objects.requireNonNull(detalle, "El detalle del pedido no puede ser nulo");

        var header = new HeaderComanda(
                detalle.numeroMesa(),
                detalle.numeroPedido(),
                LocalDateTime.now()
        );

        List<ItemComanda> items = detalle.items().stream()
                .map(this::toItemComanda)
                .toList();

        return new ComandaImpresionResponse(header, items);
    }

    private ItemTicket toItemTicket(ItemDetalleDTO item) {
        return new ItemTicket(
                item.cantidad(),
                item.nombreProducto(),
                item.precioUnitarioBase(),
                item.subtotal()
        );
    }

    private ItemComanda toItemComanda(ItemDetalleDTO item) {
        return new ItemComanda(
                item.cantidad(),
                item.nombreProducto(),
                item.observacion()
        );
    }

    /**
     * Calcula los totales del ticket separando descuentos por promoción y descuentos manuales.
     *
     * El desglose de promociones agrupa los ítems con promoción por nombre
     * y suma el ahorro de cada una.
     */
    private TotalesTicket calcularTotales(DetallePedidoResponse detalle) {
        // Descuentos de promociones: ítems que tienen promoción aplicada
        Map<String, BigDecimal> promosPorNombre = detalle.items().stream()
                .filter(ItemDetalleDTO::tienePromocion)
                .collect(Collectors.groupingBy(
                        ItemDetalleDTO::nombrePromocion,
                        Collectors.reducing(BigDecimal.ZERO, ItemDetalleDTO::descuentoTotal, BigDecimal::add)
                ));

        BigDecimal montoDescuentoPromos = promosPorNombre.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Descuentos manuales: diferencia entre total descuentos y descuentos por promo
        BigDecimal montoDescuentoManual = detalle.totalDescuentos().subtract(montoDescuentoPromos);
        if (montoDescuentoManual.compareTo(BigDecimal.ZERO) < 0) {
            montoDescuentoManual = BigDecimal.ZERO;
        }

        List<DesglosePromocion> desglose = promosPorNombre.entrySet().stream()
                .map(entry -> new DesglosePromocion(entry.getKey(), entry.getValue()))
                .toList();

        return new TotalesTicket(
                detalle.subtotal(),
                montoDescuentoPromos,
                montoDescuentoManual,
                detalle.totalParcial(),
                desglose
        );
    }
}
