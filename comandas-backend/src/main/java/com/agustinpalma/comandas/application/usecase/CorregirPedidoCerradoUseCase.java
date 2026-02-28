package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.CorreccionPedidoRequest;
import com.agustinpalma.comandas.application.dto.CorreccionPedidoRequest.ItemCorreccion;
import com.agustinpalma.comandas.application.dto.CorreccionPedidoRequest.PagoCorreccion;
import com.agustinpalma.comandas.application.dto.DetallePedidoCerradoResponse;
import com.agustinpalma.comandas.domain.model.DomainIds.ItemPedidoId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.model.Pago;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Caso de uso para corregir un pedido cerrado sin reabrir la mesa.
 * 
 * Esta es la alternativa segura a la reapertura: permite ajustar cantidades
 * de ítems y reemplazar pagos directamente sobre un pedido CERRADO,
 * sin afectar el estado de la mesa ni el flujo operativo del salón.
 * 
 * Escenario típico: el operador cerró la mesa con el medio de pago incorrecto
 * o una cantidad equivocada, y la mesa ya fue reutilizada para otro cliente.
 * 
 * Flujo:
 * 1. Buscar pedido y validar pertenencia al local
 * 2. Convertir DTOs a objetos de dominio
 * 3. pedido.corregir() → valida, recalcula snapshot, reemplaza pagos
 * 4. Persistir cambios
 * 5. Retornar detalle actualizado
 * 
 * Nota sobre stock: las diferencias de stock por cambios de cantidad
 * se registran en un futuro como MovimientoStock de tipo CORRECCION.
 * En esta primera versión, la corrección es contable (snapshot + pagos).
 */
@Transactional
public class CorregirPedidoCerradoUseCase {

    private final PedidoRepository pedidoRepository;
    private final MesaRepository mesaRepository;
    private final Clock clock;

    public CorregirPedidoCerradoUseCase(
            PedidoRepository pedidoRepository,
            MesaRepository mesaRepository,
            Clock clock
    ) {
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository, "El pedidoRepository es obligatorio");
        this.mesaRepository = Objects.requireNonNull(mesaRepository, "El mesaRepository es obligatorio");
        this.clock = Objects.requireNonNull(clock, "El clock es obligatorio");
    }

    /**
     * Ejecuta la corrección de un pedido cerrado.
     * 
     * @param localId identificador del local (tenant)
     * @param pedidoId identificador del pedido a corregir
     * @param request DTO con las correcciones de ítems y pagos
     * @return DTO con el detalle actualizado del pedido
     * @throws IllegalStateException si el pedido no existe o no está cerrado
     * @throws IllegalArgumentException si el pedido no pertenece al local o datos inválidos
     */
    public DetallePedidoCerradoResponse ejecutar(
            LocalId localId,
            PedidoId pedidoId,
            CorreccionPedidoRequest request
    ) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(pedidoId, "El pedidoId es obligatorio");
        Objects.requireNonNull(request, "El request de corrección es obligatorio");

        // 1. Recuperar pedido
        Pedido pedido = pedidoRepository.buscarPorId(pedidoId)
            .orElseThrow(() -> new IllegalStateException("El pedido no existe"));

        // 2. Validar multi-tenancy
        if (!pedido.getLocalId().equals(localId)) {
            throw new IllegalArgumentException("El pedido no pertenece a este local");
        }

        // 3. Convertir correcciones de ítems a Map de dominio
        Map<ItemPedidoId, Integer> cantidadesCorregidas = new HashMap<>();
        if (request.items() != null) {
            for (ItemCorreccion ic : request.items()) {
                cantidadesCorregidas.put(
                    new ItemPedidoId(UUID.fromString(ic.itemId())),
                    ic.cantidad()
                );
            }
        }

        // 4. Convertir pagos a Value Objects de dominio
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<Pago> nuevosPagos = request.pagos().stream()
            .map(pc -> new Pago(pc.medio(), pc.monto(), ahora))
            .toList();

        // 5. Aplicar corrección (lógica de dominio)
        pedido.corregir(cantidadesCorregidas, nuevosPagos);

        // 6. Persistir cambios
        pedidoRepository.guardar(pedido);

        // 7. Resolver mesa para el DTO de respuesta
        Mesa mesa = mesaRepository.buscarPorId(pedido.getMesaId())
            .orElseThrow(() -> new IllegalStateException("No se encontró la mesa asociada al pedido"));

        return DetallePedidoCerradoResponse.fromDomain(pedido, mesa.getNumero());
    }
}
