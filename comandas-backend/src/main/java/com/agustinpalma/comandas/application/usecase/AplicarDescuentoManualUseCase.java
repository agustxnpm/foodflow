package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.AplicarDescuentoManualRequest;
import com.agustinpalma.comandas.application.dto.AplicarDescuentoManualResponse;
import com.agustinpalma.comandas.domain.model.DescuentoManual;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPedido;
import com.agustinpalma.comandas.domain.model.ItemPedido;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso para aplicar descuento manual por porcentaje (HU-14).
 * 
 * Soporta dos modalidades:
 * 1. Descuento por ítem: afecta un producto específico dentro del pedido
 * 2. Descuento global: afecta el total del pedido
 * 
 * IMPORTANTE: El descuento manual es DINÁMICO.
 * - Solo se guarda el porcentaje (no el monto calculado)
 * - El monto se recalcula cada vez que cambia la base gravable
 * - Esto garantiza que el descuento se ajuste automáticamente al agregar/eliminar ítems
 * 
 * Jerarquía de descuentos:
 * 1° Promociones automáticas (HU-10) → Snapshot fijo
 * 2° Descuentos manuales por ítem → Dinámico sobre remanente
 * 3° Descuento global → Dinámico sobre total de ítems
 */
@Transactional
public class AplicarDescuentoManualUseCase {

    private final PedidoRepository pedidoRepository;
    private final Clock clock;

    /**
     * Constructor con inyección de dependencias.
     * 
     * @param pedidoRepository repositorio de pedidos
     * @param clock reloj del sistema para timestamp de auditoría
     */
    public AplicarDescuentoManualUseCase(
            PedidoRepository pedidoRepository,
            Clock clock
    ) {
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository, "El pedidoRepository es obligatorio");
        this.clock = Objects.requireNonNull(clock, "El clock es obligatorio");
    }

    /**
     * Ejecuta el caso de uso: aplica descuento manual a pedido o ítem.
     * 
     * Flujo:
     * 1. Recuperar pedido por ID
     * 2. Validar que el pedido está ABIERTO
     * 3. Crear DescuentoManual VO con timestamp actual
     * 4. Si es descuento por ítem → buscar ítem y aplicar
     *    Si es descuento global → aplicar directamente al pedido
     * 5. Persistir cambios
     * 6. Retornar respuesta con desglose transparente
     * 
     * @param request DTO con pedidoId, itemPedidoId (opcional), porcentaje, razón y usuarioId
     * @return DTO con desglose completo de descuentos y totales
     * @throws IllegalArgumentException si el pedido no existe o el ítem no existe
     * @throws IllegalStateException si el pedido no está ABIERTO
     */
    public AplicarDescuentoManualResponse ejecutar(AplicarDescuentoManualRequest request) {
        Objects.requireNonNull(request, "El request no puede ser null");

        // 1. Recuperar pedido
        Pedido pedido = pedidoRepository.buscarPorId(request.pedidoId())
            .orElseThrow(() -> new IllegalArgumentException(
                "No se encontró el pedido con ID: " + request.pedidoId().getValue()
            ));

        // 2. Validar que el pedido está abierto
        if (!pedido.getEstado().equals(EstadoPedido.ABIERTO)) {
            throw new IllegalStateException(
                "No se puede aplicar descuento a un pedido que no está ABIERTO. Estado actual: " + pedido.getEstado()
            );
        }

        // 3. Crear DescuentoManual VO con timestamp actual
        DescuentoManual descuento = new DescuentoManual(
            request.tipoDescuento(),
            request.valor(),
            request.razon(),
            request.usuarioId(),
            LocalDateTime.now(clock)
        );

        // 4. Aplicar descuento según modalidad
        if (request.esDescuentoPorItem()) {
            aplicarDescuentoPorItem(pedido, request.itemPedidoId().getValue(), descuento);
        } else {
            aplicarDescuentoGlobal(pedido, descuento);
        }

        // 5. Persistir cambios
        Pedido pedidoActualizado = pedidoRepository.guardar(pedido);

        // 6. Retornar respuesta con desglose transparente
        return AplicarDescuentoManualResponse.fromDomain(pedidoActualizado);
    }

    /**
     * Aplica descuento manual a un ítem específico del pedido.
     * Sobrescribe cualquier descuento manual previo del ítem.
     * 
     * @param pedido Pedido de dominio
     * @param itemPedidoId ID del ítem a descontar
     * @param descuento DescuentoManual VO
     * @throws IllegalArgumentException si el ítem no existe en el pedido
     */
    private void aplicarDescuentoPorItem(Pedido pedido, java.util.UUID itemPedidoId, DescuentoManual descuento) {
        ItemPedido item = pedido.getItems().stream()
            .filter(i -> i.getId().getValue().equals(itemPedidoId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("No se encontró el ítem %s en el pedido %s", 
                    itemPedidoId, pedido.getId().getValue())
            ));

        item.aplicarDescuentoManual(descuento);
    }

    /**
     * Aplica descuento global al pedido completo.
     * Sobrescribe cualquier descuento global previo.
     * 
     * @param pedido Pedido de dominio
     * @param descuento DescuentoManual VO
     */
    private void aplicarDescuentoGlobal(Pedido pedido, DescuentoManual descuento) {
        pedido.aplicarDescuentoGlobal(descuento);
    }
}
