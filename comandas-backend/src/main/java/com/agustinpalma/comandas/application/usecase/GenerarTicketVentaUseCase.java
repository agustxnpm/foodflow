package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.DetallePedidoResponse;
import com.agustinpalma.comandas.application.dto.AjusteEconomicoDTO;
import com.agustinpalma.comandas.application.dto.ItemDetalleDTO;
import com.agustinpalma.comandas.application.dto.ExtraDetalleDTO;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import com.agustinpalma.comandas.infrastructure.adapter.EscPosGenerator;
import com.agustinpalma.comandas.infrastructure.adapter.EscPosGenerator.TicketVentaData;
import com.agustinpalma.comandas.infrastructure.adapter.EscPosGenerator.TicketItemData;
import com.agustinpalma.comandas.infrastructure.adapter.EscPosGenerator.TicketExtraData;
import com.agustinpalma.comandas.infrastructure.config.MeisenProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * HU-29: Caso de uso para generar el ticket de venta en formato ESC/POS.
 *
 * Operación de solo lectura — no modifica estado del pedido ni de la mesa.
 *
 * Flujo:
 * 1. Delegar a ConsultarDetallePedidoUseCase para obtener datos ya calculados
 * 2. Transformar DetallePedidoResponse → TicketVentaData (datos planos para impresión)
 * 3. Generar buffer ESC/POS con EscPosGenerator.generarTicketVenta()
 * 4. Retornar Base64 para que el frontend lo envíe a la impresora vía Tauri/mock
 *
 * Decisión de diseño: reutiliza ConsultarDetallePedidoUseCase en vez de acceder
 * directamente a los repositorios. Los totales y descuentos ya están calculados
 * por el dominio y transportados por el DTO — no se recalcula nada aquí.
 */
public class GenerarTicketVentaUseCase {

    private static final Logger log = LoggerFactory.getLogger(GenerarTicketVentaUseCase.class);

    private final ConsultarDetallePedidoUseCase consultarDetallePedidoUseCase;
    private final MeisenProperties properties;

    public GenerarTicketVentaUseCase(
        ConsultarDetallePedidoUseCase consultarDetallePedidoUseCase,
        MeisenProperties properties
    ) {
        this.consultarDetallePedidoUseCase = Objects.requireNonNull(
            consultarDetallePedidoUseCase, "El consultarDetallePedidoUseCase es obligatorio"
        );
        this.properties = Objects.requireNonNull(properties, "Las properties son obligatorias");
    }

    /**
     * Genera el buffer ESC/POS del ticket de venta para una mesa.
     *
     * @param localId ID del local (tenant)
     * @param mesaId ID de la mesa
     * @return buffer ESC/POS codificado en Base64
     */
    public String ejecutar(LocalId localId, MesaId mesaId) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(mesaId, "El mesaId es obligatorio");

        // 1. Obtener datos calculados del pedido (validaciones incluidas en el use case)
        DetallePedidoResponse detalle = consultarDetallePedidoUseCase.ejecutar(localId, mesaId);

        log.info("Generando ticket de venta ESC/POS: Mesa {}, Pedido #{}, {} ítems",
            detalle.numeroMesa(), detalle.numeroPedido(), detalle.items().size());

        // 2. Transformar a datos planos para EscPosGenerator
        TicketVentaData data = construirDatosTicket(detalle);

        // 3. Generar buffer ESC/POS
        byte[] escPosBuffer = EscPosGenerator.generarTicketVenta(data);

        // 4. Codificar en Base64
        String base64 = Base64.getEncoder().encodeToString(escPosBuffer);

        log.info("Ticket de venta generado: {} bytes ESC/POS, {} chars Base64",
            escPosBuffer.length, base64.length());

        return base64;
    }

    /**
     * Transforma DetallePedidoResponse en TicketVentaData.
     *
     * Mapea los datos del DTO de aplicación a la estructura plana que
     * espera el generador ESC/POS. Agrupa extras repetidos por productoId
     * (misma lógica que TicketImpresionMapper).
     */
    private TicketVentaData construirDatosTicket(DetallePedidoResponse detalle) {
        List<TicketItemData> items = detalle.items().stream()
            .map(this::toTicketItem)
            .toList();

        // Sumar descuentos por tipo desde ajustes económicos explícitos
        BigDecimal montoPromos = detalle.ajustesEconomicos().stream()
            .filter(a -> "PROMOCION".equals(a.tipo()))
            .map(AjusteEconomicoDTO::monto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal montoManual = detalle.ajustesEconomicos().stream()
            .filter(a -> "MANUAL".equals(a.tipo()))
            .map(AjusteEconomicoDTO::monto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TicketVentaData(
            properties.getLocal().getNombreLocal(),
            properties.getLocal().getDireccion(),
            properties.getLocal().getTelefono(),
            properties.getLocal().getCuit(),
            detalle.numeroMesa(),
            detalle.numeroPedido(),
            LocalDateTime.now(),
            items,
            detalle.subtotal(),
            montoPromos,
            montoManual,
            detalle.totalParcial(),
            properties.getLocal().getMensajeBienvenida()
        );
    }

    private TicketItemData toTicketItem(ItemDetalleDTO item) {
        List<TicketExtraData> extras = agruparExtras(item.extras());

        return new TicketItemData(
            item.cantidad(),
            item.nombreProducto(),
            item.precioUnitarioBase(),
            item.subtotal(),
            extras
        );
    }

    /**
     * Agrupa extras repetidos (mismo productoId) en una sola línea con cantidad.
     * [disco, disco] → [{disco, qty: 2, subtotal: precioUnit * 2}]
     */
    private List<TicketExtraData> agruparExtras(List<ExtraDetalleDTO> extras) {
        if (extras == null || extras.isEmpty()) {
            return List.of();
        }

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
            .map(acc -> new TicketExtraData(
                acc.nombre,
                acc.cantidad,
                acc.precioUnitario.multiply(BigDecimal.valueOf(acc.cantidad))
            ))
            .toList();
    }

    private record ExtraAcumulador(String nombre, int cantidad, BigDecimal precioUnitario) {}
}
