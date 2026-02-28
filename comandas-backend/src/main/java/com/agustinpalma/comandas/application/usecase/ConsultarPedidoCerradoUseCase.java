package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.DetallePedidoCerradoResponse;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPedido;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Caso de uso para consultar el detalle completo de un pedido cerrado.
 * 
 * Usado por el modal de corrección de caja para obtener los ítems y pagos
 * antes de permitir la edición in-place.
 * 
 * Este caso de uso es de solo lectura — no modifica ningún estado.
 */
@Transactional(readOnly = true)
public class ConsultarPedidoCerradoUseCase {

    private final PedidoRepository pedidoRepository;
    private final MesaRepository mesaRepository;

    public ConsultarPedidoCerradoUseCase(
            PedidoRepository pedidoRepository,
            MesaRepository mesaRepository
    ) {
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository, "El pedidoRepository es obligatorio");
        this.mesaRepository = Objects.requireNonNull(mesaRepository, "El mesaRepository es obligatorio");
    }

    /**
     * Obtiene el detalle de un pedido cerrado para corrección.
     * 
     * @param localId identificador del local (tenant)
     * @param pedidoId identificador del pedido a consultar
     * @return DTO con ítems, pagos y montos del pedido
     * @throws IllegalStateException si el pedido no existe o no está cerrado
     * @throws IllegalArgumentException si el pedido no pertenece al local
     */
    public DetallePedidoCerradoResponse ejecutar(LocalId localId, PedidoId pedidoId) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(pedidoId, "El pedidoId es obligatorio");

        Pedido pedido = pedidoRepository.buscarPorId(pedidoId)
            .orElseThrow(() -> new IllegalStateException("El pedido no existe"));

        if (!pedido.getLocalId().equals(localId)) {
            throw new IllegalArgumentException("El pedido no pertenece a este local");
        }

        if (pedido.getEstado() != EstadoPedido.CERRADO) {
            throw new IllegalStateException(
                String.format("Solo se pueden consultar pedidos cerrados para corrección. Estado actual: %s",
                    pedido.getEstado())
            );
        }

        Mesa mesa = mesaRepository.buscarPorId(pedido.getMesaId())
            .orElseThrow(() -> new IllegalStateException("No se encontró la mesa asociada al pedido"));

        return DetallePedidoCerradoResponse.fromDomain(pedido, mesa.getNumero());
    }
}
