package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.AbrirMesaRequest;
import com.agustinpalma.comandas.application.dto.AbrirMesaResponse;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPedido;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Caso de uso para abrir una mesa libre y crear su pedido inicial.
 * Orquesta la lógica de aplicación cumpliendo con las invariantes del dominio.
 * 
 * Criterios de aceptación implementados:
 * 1. Solo se pueden abrir mesas libres
 * 2. Al abrirse, se crea un pedido en estado ABIERTO
 * 3. La mesa debe pertenecer al local del usuario
 * 4. Una mesa solo puede tener un único pedido ABIERTO a la vez
 * 5. El pedido queda vinculado inmutablemente a MesaId y LocalId
 */
@Service
public class AbrirMesaUseCase {

    private final MesaRepository mesaRepository;
    private final PedidoRepository pedidoRepository;

    /**
     * Constructor con inyección de dependencias.
     * El framework de infraestructura (Spring) se encargará de proveer las implementaciones.
     *
     * @param mesaRepository repositorio de mesas
     * @param pedidoRepository repositorio de pedidos
     */
    public AbrirMesaUseCase(MesaRepository mesaRepository, PedidoRepository pedidoRepository) {
        this.mesaRepository = Objects.requireNonNull(mesaRepository, "El mesaRepository es obligatorio");
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository, "El pedidoRepository es obligatorio");
    }

    /**
     * Ejecuta el caso de uso: abrir una mesa y crear su pedido.
     *
     * @param localId identificador del local (tenant) del usuario autenticado
     * @param request DTO con el ID de la mesa a abrir
     * @return DTO con la información de la mesa abierta y el pedido creado
     * @throws IllegalArgumentException si la validación falla
     * @throws IllegalStateException si la mesa no se encuentra
     */
    public AbrirMesaResponse ejecutar(LocalId localId, AbrirMesaRequest request) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(request, "El request es obligatorio");

        MesaId mesaId = new MesaId(UUID.fromString(request.mesaId()));

        // 1. Recuperar la mesa
        Mesa mesa = mesaRepository.buscarPorId(mesaId)
            .orElseThrow(() -> new IllegalStateException("La mesa no existe"));

        // 2. Validar que pertenece al local del usuario (multi-tenancy)
        if (!mesa.getLocalId().equals(localId)) {
            throw new IllegalArgumentException("La mesa no pertenece a este local");
        }

        // 3. Validar que no existe otro pedido abierto para esta mesa
        pedidoRepository.buscarPorMesaYEstado(mesaId, EstadoPedido.ABIERTO)
            .ifPresent(pedidoExistente -> {
                throw new IllegalStateException("La mesa ya tiene un pedido abierto");
            });

        // 4. Abrir la mesa (la entidad valida que esté LIBRE)
        mesa.abrir();

        // 5. Crear el nuevo pedido en estado ABIERTO vinculado al local (multi-tenancy)
        Pedido nuevoPedido = new Pedido(
            new PedidoId(UUID.randomUUID()),
            localId,
            mesaId,
            EstadoPedido.ABIERTO,
            LocalDateTime.now()
        );

        // 6. Persistir los cambios (orden: primero pedido, luego mesa)
        Pedido pedidoGuardado = pedidoRepository.guardar(nuevoPedido);
        Mesa mesaGuardada = mesaRepository.guardar(mesa);

        // 7. Retornar DTO de respuesta
        return AbrirMesaResponse.fromDomain(mesaGuardada, pedidoGuardado);
    }
}
