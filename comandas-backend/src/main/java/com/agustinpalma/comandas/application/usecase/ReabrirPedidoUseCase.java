package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.ReabrirPedidoResponse;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import com.agustinpalma.comandas.domain.model.ItemPedido;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.model.MovimientoStock;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.domain.repository.MovimientoStockRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import com.agustinpalma.comandas.domain.service.GestorStockService;
import com.agustinpalma.comandas.domain.service.GestorStockService.ResultadoStock;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * HU-14: Caso de uso para reabrir un pedido previamente cerrado.
 * 
 * Este caso de uso implementa una "válvula de escape" operativa que permite
 * corregir errores humanos antes del cierre de caja definitivo.
 * 
 * Ejemplos de uso:
 * - Se cobró en efectivo cuando era tarjeta
 * - Se cerró la mesa equivocada
 * - Se olvidó agregar un ítem antes del cierre
 * 
 * La operación es atómica y revierte completamente el cierre:
 * - Pedido: CERRADO → ABIERTO
 * - Mesa: LIBRE → ABIERTA
 * - Snapshot contable: eliminado (montos vuelven a null)
 * - Pagos: eliminados físicamente (orphanRemoval en JPA)
 * 
 * Flujo:
 * 1. Buscar el Pedido por ID y validar tenant
 * 2. Buscar la Mesa asociada al pedido
 * 3. HU-22: Revertir stock ANTES de reabrir (el pedido aún tiene sus ítems)
 * 4. pedido.reabrir() → revierte estado y limpia snapshot/pagos
 * 5. mesa.reocupar() → devuelve la mesa a ABIERTA
 * 6. Persistir cambios (transacción atómica)
 * 
 * ADVERTENCIA: Esta operación es destructiva. Los pagos previos se eliminan.
 * Solo debe usarse para correcciones excepcionales antes del cierre de caja.
 */
@Transactional
public class ReabrirPedidoUseCase {

    private final PedidoRepository pedidoRepository;
    private final MesaRepository mesaRepository;
    private final ProductoRepository productoRepository;
    private final MovimientoStockRepository movimientoStockRepository;
    private final GestorStockService gestorStockService;
    private final Clock clock;

    public ReabrirPedidoUseCase(
            PedidoRepository pedidoRepository,
            MesaRepository mesaRepository,
            ProductoRepository productoRepository,
            MovimientoStockRepository movimientoStockRepository,
            GestorStockService gestorStockService,
            Clock clock
    ) {
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository, "El pedidoRepository es obligatorio");
        this.mesaRepository = Objects.requireNonNull(mesaRepository, "El mesaRepository es obligatorio");
        this.productoRepository = Objects.requireNonNull(productoRepository, "El productoRepository es obligatorio");
        this.movimientoStockRepository = Objects.requireNonNull(movimientoStockRepository, "El movimientoStockRepository es obligatorio");
        this.gestorStockService = Objects.requireNonNull(gestorStockService, "El gestorStockService es obligatorio");
        this.clock = Objects.requireNonNull(clock, "El clock es obligatorio");
    }

    /**
     * Ejecuta el caso de uso: reabre un pedido cerrado y reocupa su mesa.
     *
     * @param localId identificador del local (tenant)
     * @param pedidoId identificador del pedido a reabrir
     * @return DTO con la información del pedido reabierto y la mesa reocupada
     * @throws IllegalStateException si el pedido no existe, no está CERRADO, o la mesa no está LIBRE
     * @throws IllegalArgumentException si el pedido no pertenece al local
     */
    public ReabrirPedidoResponse ejecutar(LocalId localId, PedidoId pedidoId) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(pedidoId, "El pedidoId es obligatorio");

        // 1. Buscar el pedido
        Pedido pedido = pedidoRepository.buscarPorId(pedidoId)
            .orElseThrow(() -> new IllegalStateException(
                String.format("No existe un pedido con ID %s", pedidoId.getValue())
            ));

        // 2. Validar multi-tenancy
        if (!pedido.getLocalId().equals(localId)) {
            throw new IllegalArgumentException(
                String.format("El pedido %s no pertenece al local %s",
                    pedidoId.getValue(), localId.getValue())
            );
        }

        // 3. Buscar la mesa asociada
        Mesa mesa = mesaRepository.buscarPorId(pedido.getMesaId())
            .orElseThrow(() -> new IllegalStateException(
                String.format("No existe la mesa con ID %s asociada al pedido",
                    pedido.getMesaId().getValue())
            ));

        // 4. Timestamp de la reapertura
        LocalDateTime ahora = LocalDateTime.now(clock);

        // 5. HU-22: Revertir stock ANTES de reabrir
        //    (el pedido aún está CERRADO y tiene sus ítems intactos)
        revertirStockPorReapertura(pedido, localId, ahora);

        // 6. Reabrir el pedido (valida estado CERRADO, limpia snapshot y pagos)
        pedido.reabrir(ahora);

        // 7. Reocupar la mesa (valida estado LIBRE, vuelve a ABIERTA)
        mesa.reocupar();

        // 8. Persistir cambios (transacción atómica: si falla uno, falla todo)
        pedidoRepository.guardar(pedido);
        mesaRepository.guardar(mesa);

        // 9. Retornar DTO de respuesta
        return ReabrirPedidoResponse.fromDomain(mesa, pedido, ahora);
    }

    /**
     * HU-22: Revierte el stock descontado por la venta original.
     * Carga los productos involucrados, delega al GestorStockService,
     * y persiste los cambios y movimientos de auditoría.
     */
    private void revertirStockPorReapertura(Pedido pedido, LocalId localId, LocalDateTime fecha) {
        Map<ProductoId, Producto> productosDelPedido = pedido.getItems().stream()
            .map(ItemPedido::getProductoId)
            .distinct()
            .map(productoId -> productoRepository.buscarPorIdYLocal(productoId, localId).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Producto::getId, p -> p));

        if (productosDelPedido.isEmpty()) {
            return;
        }

        ResultadoStock resultado = gestorStockService.revertirVenta(pedido, productosDelPedido, fecha);

        for (Producto producto : resultado.productosModificados()) {
            productoRepository.guardar(producto);
        }

        for (MovimientoStock movimiento : resultado.movimientos()) {
            movimientoStockRepository.guardar(movimiento);
        }
    }
}
