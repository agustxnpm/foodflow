package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.AjusteEconomicoDTO;
import com.agustinpalma.comandas.application.dto.DetallePedidoResponse;
import com.agustinpalma.comandas.application.dto.ExtraDetalleDTO;
import com.agustinpalma.comandas.application.dto.ItemDetalleDTO;
import com.agustinpalma.comandas.domain.model.AjusteEconomico;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoMesa;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import com.agustinpalma.comandas.domain.model.ItemPedido;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Caso de uso: Ver detalle del pedido activo de una mesa.
 * 
 * Regla de Oro del Dominio aplicada:
 * "El total del pedido se calcula a partir de los ítems base"
 * Los cálculos financieros SIEMPRE se ejecutan en el dominio (Pedido/ItemPedido),
 * nunca en los DTOs ni en el caso de uso.
 */
@Transactional(readOnly = true)
public class ConsultarDetallePedidoUseCase {

    private final MesaRepository mesaRepository;
    private final PedidoRepository pedidoRepository;

    /**
     * Constructor con inyección de dependencias.
     * El framework de infraestructura (Spring) se encargará de proveer las implementaciones.
     *
     * @param mesaRepository repositorio de mesas
     * @param pedidoRepository repositorio de pedidos
     */
    public ConsultarDetallePedidoUseCase(
        MesaRepository mesaRepository, 
        PedidoRepository pedidoRepository
    ) {
        this.mesaRepository = Objects.requireNonNull(mesaRepository, "El mesaRepository es obligatorio");
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository, "El pedidoRepository es obligatorio");
    }

    /**
     * Ejecuta el caso de uso: consultar detalle del pedido activo de una mesa.
     * 
     * Flujo:
     * 1. Buscar la mesa por ID
     * 2. Validar seguridad (multi-tenancy)
     * 3. Validar que la mesa esté ABIERTA
     * 4. Buscar el pedido activo
     * 5. Mapear a DTO usando los cálculos del dominio
     *
     * @param localId identificador del local (tenant) del usuario autenticado
     * @param mesaId identificador de la mesa a consultar
     * @return DTO con el detalle completo del pedido
     * @throws IllegalStateException si la mesa no existe, está LIBRE, o no tiene pedido abierto
     * @throws IllegalArgumentException si la mesa no pertenece al local del usuario
     */
    public DetallePedidoResponse ejecutar(LocalId localId, MesaId mesaId) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(mesaId, "El mesaId es obligatorio");

        // 1. Buscar la mesa
        Mesa mesa = mesaRepository.buscarPorId(mesaId)
            .orElseThrow(() -> new IllegalStateException("La mesa no existe"));

        // 2. AC5 - Validar seguridad: la mesa debe pertenecer al local del usuario
        if (!mesa.getLocalId().equals(localId)) {
            throw new IllegalArgumentException("La mesa no pertenece a este local");
        }

        // 3. AC4 - Validar estado: la mesa debe estar ABIERTA
        if (mesa.getEstado() == EstadoMesa.LIBRE) {
            throw new IllegalStateException("La mesa no tiene un pedido activo");
        }

        // 4. Buscar el pedido abierto de la mesa (con aislamiento multi-tenant)
        Pedido pedido = pedidoRepository.buscarAbiertoPorMesa(mesaId, localId)
            .orElseThrow(() -> new IllegalStateException(
                "La mesa está marcada como ABIERTA pero no tiene un pedido activo. Inconsistencia de datos."
            ));

        // 5. Mapear a DTO usando los cálculos del dominio
        return mapearADetalle(pedido, mesa.getNumero());
    }

    /**
     * Mapea el pedido de dominio a DTO de respuesta.
     * 
     * REGLA CRÍTICA:
     * Los cálculos (subtotales, total) se invocan desde el dominio.
     * Este método NO realiza cálculos, solo transporta valores ya calculados.
     * 
     * @param pedido entidad de dominio con los datos del pedido
     * @param numeroMesa número de la mesa (para incluirlo en el contexto)
     * @return DTO con toda la información del pedido
     */
    private DetallePedidoResponse mapearADetalle(Pedido pedido, int numeroMesa) {
        // AC1 - Mapear ítems con toda su información
        List<ItemDetalleDTO> itemsDTO = pedido.getItems().stream()
            .map(this::mapearItem)
            .toList();

        // Ajustes económicos explícitos desde el dominio — la narrativa del pedido.
        // Cada ajuste describe un descuento concreto con su origen, razón y monto.
        List<AjusteEconomico> ajustes = pedido.obtenerAjustesEconomicos();
        List<AjusteEconomicoDTO> ajustesDTO = ajustes.stream()
            .map(a -> new AjusteEconomicoDTO(
                a.getTipo().name(),
                a.getAmbito().name(),
                a.getDescripcion(),
                a.getMonto()
            ))
            .toList();

        // Totales derivados de los ajustes explícitos — sin inferencia subtotal - total.
        // totalDescuentos = suma de cada ajuste materializado por el dominio.
        var subtotal = pedido.calcularSubtotalItems();
        var totalDescuentos = ajustes.stream()
            .map(AjusteEconomico::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        var totalParcial = subtotal.subtract(totalDescuentos);

        // AC3 - Construir respuesta con información de contexto
        return new DetallePedidoResponse(
            pedido.getId().getValue().toString(),
            pedido.getNumero(),
            numeroMesa,
            pedido.getEstado().name(),
            pedido.getFechaApertura(),
            itemsDTO,
            subtotal,
            totalDescuentos,
            totalParcial,
            ajustesDTO
        );
    }

    /**
     * Mapea un ítem de dominio a DTO.
     * 
     * HU-10: Incluye información de promoción aplicada.
     * HU-05.1 + HU-22: Incluye extras como sub-elementos del ítem.
     * 
     * REGLA CRÍTICA:
     * Los cálculos se invocan desde el dominio (calcularSubtotal, calcularPrecioFinal).
     * Este método NO calcula, solo transporta el resultado.
     * 
     * @param item entidad de dominio ItemPedido
     * @return DTO con la información del ítem
     */
    private ItemDetalleDTO mapearItem(ItemPedido item) {
        // Descuentos explícitos del dominio: promo (snapshot) + manual (calculado desde VO).
        // No se infiere por resta subtotal - precioFinal.
        var subtotal = item.calcularSubtotalLinea();
        var precioFinal = item.calcularPrecioFinal();
        var descuentoTotal = item.getMontoDescuento()
            .add(item.calcularMontoDescuentoManual());

        // Mapear extras de dominio a DTO (sub-elementos del ítem)
        List<ExtraDetalleDTO> extrasDTO = item.getExtras().stream()
            .map(extra -> new ExtraDetalleDTO(
                extra.getProductoId().getValue().toString(),
                extra.getNombre(),
                extra.getPrecioSnapshot()
            ))
            .toList();

        return new ItemDetalleDTO(
            item.getId().getValue().toString(),
            item.getNombreProducto(),
            item.getCantidad(),
            item.getPrecioUnitario(),
            subtotal,
            descuentoTotal,
            precioFinal,
            item.getObservacion(),
            item.getNombrePromocion(),
            item.tienePromocion() || item.tieneDescuentoManual(),
            extrasDTO
        );
    }
}
