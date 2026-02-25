package com.agustinpalma.comandas.infrastructure.mapper;

import com.agustinpalma.comandas.application.dto.AjusteEconomicoDTO;
import com.agustinpalma.comandas.application.dto.ComandaImpresionResponse;
import com.agustinpalma.comandas.application.dto.ComandaImpresionResponse.HeaderComanda;
import com.agustinpalma.comandas.application.dto.ComandaImpresionResponse.ItemComanda;
import com.agustinpalma.comandas.application.dto.DetallePedidoResponse;
import com.agustinpalma.comandas.application.dto.ItemDetalleDTO;
import com.agustinpalma.comandas.application.dto.TicketImpresionResponse;
import com.agustinpalma.comandas.application.dto.TicketImpresionResponse.DesgloseAjuste;
import com.agustinpalma.comandas.application.dto.TicketImpresionResponse.FooterTicket;
import com.agustinpalma.comandas.application.dto.TicketImpresionResponse.HeaderTicket;
import com.agustinpalma.comandas.application.dto.TicketImpresionResponse.ItemTicket;
import com.agustinpalma.comandas.application.dto.TicketImpresionResponse.TotalesTicket;
import com.agustinpalma.comandas.infrastructure.config.MeisenProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
        // Agrupar extras repetidos (mismo productoId) → "2x Disco de carne" en vez de 2 líneas
        List<TicketImpresionResponse.ExtraTicket> extrasAgrupados = agruparExtras(item.extras());

        return new ItemTicket(
                item.cantidad(),
                item.nombreProducto(),
                item.precioUnitarioBase(),
                item.subtotal(),
                extrasAgrupados
        );
    }

    /**
     * Agrupa extras repetidos (mismo productoId) en una sola línea con cantidad.
     * El backend envía extras individuales: [disco, disco] → [{disco, qty: 2}]
     * Mantiene el orden de aparición original.
     */
    private List<TicketImpresionResponse.ExtraTicket> agruparExtras(List<com.agustinpalma.comandas.application.dto.ExtraDetalleDTO> extras) {
        if (extras == null || extras.isEmpty()) {
            return List.of();
        }

        // LinkedHashMap para mantener orden de inserción
        Map<String, ExtraAcumulador> mapa = new LinkedHashMap<>();
        for (var extra : extras) {
            mapa.merge(
                    extra.productoId(),
                    new ExtraAcumulador(extra.nombre(), 1, extra.precio()),
                    (existente, nuevo) -> new ExtraAcumulador(
                            existente.nombre, existente.cantidad + 1, existente.precioUnitario
                    )
            );
        }

        return mapa.values().stream()
                .map(acc -> new TicketImpresionResponse.ExtraTicket(
                        acc.nombre,
                        acc.cantidad,
                        acc.precioUnitario,
                        acc.precioUnitario.multiply(BigDecimal.valueOf(acc.cantidad))
                ))
                .toList();
    }

    /** Acumulador interno para agrupar extras por productoId */
    private record ExtraAcumulador(String nombre, int cantidad, BigDecimal precioUnitario) {}

    private ItemComanda toItemComanda(ItemDetalleDTO item) {
        return new ItemComanda(
                item.cantidad(),
                item.nombreProducto(),
                item.observacion()
        );
    }

    /**
     * Construye los totales del ticket a partir de los ajustes económicos explícitos.
     *
     * No infiere descuentos. No resta valores. Solo agrupa y suma datos
     * ya materializados por el dominio y transportados por el DTO.
     */
    private TotalesTicket calcularTotales(DetallePedidoResponse detalle) {
        var ajustes = detalle.ajustesEconomicos();

        // Sumar montos por tipo — datos explícitos, sin inferencia
        BigDecimal montoPromos = ajustes.stream()
                .filter(a -> "PROMOCION".equals(a.tipo()))
                .map(AjusteEconomicoDTO::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal montoManual = ajustes.stream()
                .filter(a -> "MANUAL".equals(a.tipo()))
                .map(AjusteEconomicoDTO::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Promos: agrupar por descripción (ej: "2x1 Cervezas" en 2 ítems → 1 línea)
        List<DesgloseAjuste> desglosePromos = ajustes.stream()
                .filter(a -> "PROMOCION".equals(a.tipo()))
                .collect(Collectors.groupingBy(
                        AjusteEconomicoDTO::descripcion,
                        Collectors.reducing(BigDecimal.ZERO, AjusteEconomicoDTO::monto, BigDecimal::add)
                ))
                .entrySet().stream()
                .map(e -> new DesgloseAjuste("PROMOCION", e.getKey(), e.getValue()))
                .toList();

        // Manuales: listar individualmente con razón
        List<DesgloseAjuste> desgloseManuales = ajustes.stream()
                .filter(a -> "MANUAL".equals(a.tipo()))
                .map(a -> new DesgloseAjuste("MANUAL", a.descripcion(), a.monto()))
                .toList();

        // Combinar: promos primero, manuales después
        var desgloseCompleto = new ArrayList<DesgloseAjuste>(desglosePromos);
        desgloseCompleto.addAll(desgloseManuales);

        return new TotalesTicket(
                detalle.subtotal(),
                montoPromos,
                montoManual,
                detalle.totalParcial(),
                List.copyOf(desgloseCompleto)
        );
    }
}
