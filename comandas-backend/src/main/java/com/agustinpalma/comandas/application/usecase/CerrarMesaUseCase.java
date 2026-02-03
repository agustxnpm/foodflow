package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.CerrarMesaRequest;
import com.agustinpalma.comandas.application.dto.CerrarMesaResponse;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

/**
 * Caso de uso para cerrar una mesa abierta y finalizar su pedido activo.
 * Orquesta la lógica de aplicación cumpliendo con las invariantes del dominio.
 *
 * Criterios de aceptación implementados:
 * 1. Solo se pueden cerrar mesas que tengan un pedido ABIERTO
 * 2. El cierre implica la transición del pedido a CERRADO
 * 3. Se debe registrar el medio de pago obligatoriamente
 * 4. La mesa vuelve a estado LIBRE
 * 5. Multi-tenancy: la mesa debe pertenecer al local del usuario
 * 6. No se permite cerrar pedidos sin ítems
 * 7. Se registra el timestamp de cierre para auditoría
 *
 * La operación es atómica: o se cierran ambos (Mesa y Pedido) o ninguno.
 */
@Transactional
public class CerrarMesaUseCase {

    private final MesaRepository mesaRepository;
    private final PedidoRepository pedidoRepository;

    /**
     * Constructor con inyección de dependencias.
     * El framework de infraestructura (Spring) se encargará de proveer las implementaciones.
     *
     * @param mesaRepository repositorio de mesas
     * @param pedidoRepository repositorio de pedidos
     */
    public CerrarMesaUseCase(MesaRepository mesaRepository, PedidoRepository pedidoRepository) {
        this.mesaRepository = Objects.requireNonNull(mesaRepository, "El mesaRepository es obligatorio");
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository, "El pedidoRepository es obligatorio");
    }

    /**
     * Ejecuta el caso de uso: cerrar la mesa y finalizar su pedido activo.
     *
     * @param localId identificador del local (tenant) del usuario autenticado
     * @param request DTO con el ID de la mesa a cerrar y el medio de pago
     * @return DTO con la información de la mesa liberada y el pedido cerrado
     * @throws IllegalArgumentException si la validación falla (mesa no pertenece al local, medio de pago inválido)
     * @throws IllegalStateException si la mesa no existe, no tiene pedido abierto, o el pedido no tiene ítems
     */
    public CerrarMesaResponse ejecutar(LocalId localId, CerrarMesaRequest request) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(request, "El request es obligatorio");
        Objects.requireNonNull(request.medioPago(), "El medio de pago es obligatorio");

        MesaId mesaId = new MesaId(UUID.fromString(request.mesaId()));

        // 1. Recuperar la mesa
        Mesa mesa = mesaRepository.buscarPorId(mesaId)
            .orElseThrow(() -> new IllegalStateException("La mesa no existe"));

        // 2. Validar que pertenece al local del usuario (multi-tenancy)
        if (!mesa.getLocalId().equals(localId)) {
            throw new IllegalArgumentException("La mesa no pertenece a este local");
        }

        // 3. Buscar el pedido abierto asociado a la mesa
        Pedido pedido = pedidoRepository.buscarAbiertoPorMesa(mesaId)
            .orElseThrow(() -> new IllegalStateException("La mesa no tiene un pedido abierto"));

        // 4. Finalizar el pedido (valida estado ABIERTO y que tenga ítems)
        //    El dominio se encarga de validar las reglas de negocio
        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires"));
        pedido.finalizar(request.medioPago(), ahora);

        // 5. Cerrar la mesa (valida estado ABIERTA)
        mesa.cerrar();

        // 6. Persistir los cambios (orden: primero pedido, luego mesa)
        Pedido pedidoCerrado = pedidoRepository.guardar(pedido);
        Mesa mesaLiberada = mesaRepository.guardar(mesa);

        // 7. Retornar DTO de respuesta
        return CerrarMesaResponse.fromDomain(mesaLiberada, pedidoCerrado);
    }
}
