package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.AgregarProductoResponse;
import com.agustinpalma.comandas.application.dto.EliminarItemPedidoRequest;
import com.agustinpalma.comandas.application.dto.ModificarCantidadItemRequest;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.model.Promocion;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import com.agustinpalma.comandas.domain.repository.PromocionRepository;
import com.agustinpalma.comandas.domain.service.MotorReglasService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso atómico para gestionar ítems en un pedido abierto.
 * 
 * HU-20: Eliminar producto de un pedido abierto.
 * HU-21: Modificar cantidad de un producto en pedido abierto.
 * 
 * Regla fundamental:
 * Cualquier cambio en los ítems dispara un recálculo total del pedido, incluyendo:
 * - Promociones automáticas (HU-10)
 * - Promociones en ciclos NxM
 * - Combos condicionales
 * - El descuento manual global se recalcula dinámicamente al generar la respuesta (HU-14)
 * 
 * Flujo crítico de recálculo:
 * 1. Recuperar el Pedido por ID
 * 2. Ejecutar la mutación en el dominio (actualizar cantidad o eliminar)
 * 3. Resetear promociones previas de TODOS los ítems
 * 4. Re-evaluar promociones automáticas con MotorReglasService
 * 5. Persistir el Pedido
 * 
 * NOTA: El descuento global (HU-14) NO se recalcula manualmente aquí.
 * Se recalcula dinámicamente al generar la respuesta via calcularTotal().
 */
@Transactional
public class GestionarItemsPedidoUseCase {

    private static final Logger log = LoggerFactory.getLogger(GestionarItemsPedidoUseCase.class);
    
    private final PedidoRepository pedidoRepository;
    private final PromocionRepository promocionRepository;
    private final MotorReglasService motorReglasService;
    private final Clock clock;

    public GestionarItemsPedidoUseCase(
            PedidoRepository pedidoRepository,
            PromocionRepository promocionRepository,
            MotorReglasService motorReglasService,
            Clock clock
    ) {
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository, "El pedidoRepository es obligatorio");
        this.promocionRepository = Objects.requireNonNull(promocionRepository, "El promocionRepository es obligatorio");
        this.motorReglasService = Objects.requireNonNull(motorReglasService, "El motorReglasService es obligatorio");
        this.clock = Objects.requireNonNull(clock, "El clock es obligatorio");
    }

    /**
     * HU-21: Modifica la cantidad de un ítem en el pedido.
     * 
     * La interpretación semántica de la cantidad se delega al dominio:
     * - cantidad == actual → idempotente (no recalcula)
     * - cantidad == 0     → elimina el ítem
     * - cantidad < 0      → rechazado
     * - cantidad > 0      → actualiza y recalcula promociones
     * 
     * @param request DTO con pedidoId, itemPedidoId y nueva cantidad
     * @return el pedido actualizado con recálculo completo
     * @throws IllegalArgumentException si el pedido o ítem no existe
     * @throws IllegalStateException si el pedido no está ABIERTO
     */
    public AgregarProductoResponse modificarCantidad(ModificarCantidadItemRequest request) {
        Objects.requireNonNull(request, "El request no puede ser null");
        
        log.info("Modificando cantidad: pedidoId={}, itemId={}, nuevaCantidad={}",
                request.pedidoId().getValue(), request.itemPedidoId().getValue(), request.cantidad());

        // 1. Recuperar Pedido
        Pedido pedido = pedidoRepository.buscarPorId(request.pedidoId())
            .orElseThrow(() -> new IllegalArgumentException(
                "No se encontró el pedido con ID: " + request.pedidoId().getValue()
            ));
        
        log.debug("Pedido recuperado: items={}", pedido.getItems().size());

        // Guardar cantidad actual para detectar idempotencia
        int cantidadAnterior = pedido.getItems().stream()
            .filter(item -> item.getId().equals(request.itemPedidoId()))
            .findFirst()
            .map(item -> item.getCantidad())
            .orElse(-1); // -1 indica que no se encontró (el dominio lanzará la excepción)

        // 2. Ejecutar mutación en el dominio
        pedido.actualizarCantidadItem(request.itemPedidoId(), request.cantidad());

        // Idempotencia: si la cantidad no cambió, retornar sin recalcular
        if (cantidadAnterior == request.cantidad()) {
            return AgregarProductoResponse.fromDomain(pedido);
        }

        // 3-4. Recalcular promociones (si quedan ítems)
        recalcularPromociones(pedido);

        // 5. Persistir
        Pedido pedidoActualizado = pedidoRepository.guardar(pedido);
        
        log.info("Cantidad modificada exitosamente. Items resultantes: {}", pedidoActualizado.getItems().size());

        return AgregarProductoResponse.fromDomain(pedidoActualizado);
    }

    /**
     * HU-20: Elimina un ítem del pedido.
     * 
     * Tras eliminar, se recalculan todas las promociones del pedido.
     * Esto es crítico para combos: eliminar el trigger hace que el target
     * pierda su descuento automáticamente.
     * 
     * @param request DTO con pedidoId y itemPedidoId
     * @return el pedido actualizado con recálculo completo
     * @throws IllegalArgumentException si el pedido o ítem no existe
     * @throws IllegalStateException si el pedido no está ABIERTO
     */
    public AgregarProductoResponse eliminarItem(EliminarItemPedidoRequest request) {
        Objects.requireNonNull(request, "El request no puede ser null");
        
        log.info("Eliminando item: pedidoId={}, itemId={}",
                request.pedidoId().getValue(), request.itemPedidoId().getValue());

        // 1. Recuperar Pedido
        Pedido pedido = pedidoRepository.buscarPorId(request.pedidoId())
            .orElseThrow(() -> new IllegalArgumentException(
                "No se encontró el pedido con ID: " + request.pedidoId().getValue()
            ));
        
        log.debug("Pedido recuperado antes de eliminar: items={}", pedido.getItems().size());

        // 2. Ejecutar eliminación en el dominio
        pedido.eliminarItem(request.itemPedidoId());
        
        log.debug("Item eliminado del dominio. Items restantes: {}", pedido.getItems().size());

        // 3-4. Recalcular promociones (si quedan ítems)
        recalcularPromociones(pedido);

        // 5. Persistir
        Pedido pedidoActualizado = pedidoRepository.guardar(pedido);
        
        log.info("Item eliminado exitosamente. Items resultantes: {}", pedidoActualizado.getItems().size());

        return AgregarProductoResponse.fromDomain(pedidoActualizado);
    }

    /**
     * Recalcula las promociones automáticas de todo el pedido.
     * 
     * Flujo:
     * 1. Limpiar promociones previas de todos los ítems
     * 2. Recuperar promociones activas del local
     * 3. Invocar MotorReglasService.aplicarPromociones() para re-evaluar
     * 
     * NOTA: Solo recalcula si quedan ítems en el pedido.
     * Un pedido vacío no tiene promociones que evaluar.
     */
    private void recalcularPromociones(Pedido pedido) {
        if (pedido.getItems().isEmpty()) {
            return;
        }

        // Limpiar todas las promociones previas
        pedido.limpiarPromocionesItems();

        // Recuperar promociones activas del local
        List<Promocion> promocionesActivas = promocionRepository.buscarActivasPorLocal(pedido.getLocalId());

        // Re-evaluar promociones con el motor de reglas
        motorReglasService.aplicarPromociones(
            pedido,
            promocionesActivas,
            LocalDateTime.now(clock)
        );
    }
}
