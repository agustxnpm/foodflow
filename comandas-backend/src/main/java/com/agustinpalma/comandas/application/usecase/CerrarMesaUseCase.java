package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.CerrarMesaResponse;
import com.agustinpalma.comandas.application.dto.PagoRequest;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.model.Pago;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.model.Promocion;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import com.agustinpalma.comandas.domain.repository.PromocionRepository;
import com.agustinpalma.comandas.domain.service.MotorReglasService;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Caso de uso para cerrar una mesa abierta y finalizar su pedido activo.
 * 
 * El cierre de mesa es un evento de negocio crítico que consolida:
 * - Cierre de Pedido (inmutabilidad financiera via snapshot contable)
 * - Registro de Pagos (soporte pagos parciales / split)
 * - Cierre de Mesa (liberación del recurso físico)
 * 
 * La operación es atómica: o se ejecuta todo o nada.
 * 
 * Flujo:
 * 1. Recuperar Mesa y su Pedido activo
 * 2. Re-evaluar promociones una última vez (MotorReglasService)
 * 3. pedido.cerrar(pagos) → congela snapshot + valida montos
 * 4. mesa.liberar() → devuelve la mesa a estado LIBRE
 * 5. Persistir cambios
 */
@Transactional
public class CerrarMesaUseCase {

    private final MesaRepository mesaRepository;
    private final PedidoRepository pedidoRepository;
    private final PromocionRepository promocionRepository;
    private final MotorReglasService motorReglasService;
    private final Clock clock;

    public CerrarMesaUseCase(
            MesaRepository mesaRepository,
            PedidoRepository pedidoRepository,
            PromocionRepository promocionRepository,
            MotorReglasService motorReglasService,
            Clock clock
    ) {
        this.mesaRepository = Objects.requireNonNull(mesaRepository, "El mesaRepository es obligatorio");
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository, "El pedidoRepository es obligatorio");
        this.promocionRepository = Objects.requireNonNull(promocionRepository, "El promocionRepository es obligatorio");
        this.motorReglasService = Objects.requireNonNull(motorReglasService, "El motorReglasService es obligatorio");
        this.clock = Objects.requireNonNull(clock, "El clock es obligatorio");
    }

    /**
     * Ejecuta el caso de uso: cerrar la mesa y finalizar su pedido activo.
     *
     * @param localId identificador del local (tenant)
     * @param mesaId identificador de la mesa a cerrar
     * @param pagos lista de pagos para cubrir el total
     * @return DTO con la información de la mesa liberada y el pedido cerrado
     * @throws IllegalStateException si la mesa no existe o no tiene pedido abierto
     * @throws IllegalArgumentException si la mesa no pertenece al local o los pagos son inválidos
     */
    public CerrarMesaResponse ejecutar(LocalId localId, MesaId mesaId, List<PagoRequest> pagos) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(mesaId, "El mesaId es obligatorio");
        Objects.requireNonNull(pagos, "La lista de pagos es obligatoria");

        // 1. Recuperar la mesa
        Mesa mesa = mesaRepository.buscarPorId(mesaId)
            .orElseThrow(() -> new IllegalStateException("La mesa no existe"));

        // 2. Validar multi-tenancy
        if (!mesa.getLocalId().equals(localId)) {
            throw new IllegalArgumentException("La mesa no pertenece a este local");
        }

        // 3. Buscar el pedido abierto asociado a la mesa
        Pedido pedido = pedidoRepository.buscarAbiertoPorMesa(mesaId, localId)
            .orElseThrow(() -> new IllegalStateException("La mesa no tiene un pedido abierto"));

        // 4. Re-evaluar promociones una última vez antes del cierre
        aplicarPromocionesFinales(pedido, localId);

        // 5. Convertir DTOs de pago a Value Objects de dominio
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<Pago> pagosDominio = pagos.stream()
            .map(pr -> new Pago(pr.medio(), pr.monto(), ahora))
            .toList();

        // 6. Cerrar el pedido (valida estado, ítems, montos; congela snapshot)
        pedido.cerrar(pagosDominio, ahora);

        // 7. Liberar la mesa
        mesa.liberar();

        // 8. Persistir cambios (transacción atómica)
        pedidoRepository.guardar(pedido);
        mesaRepository.guardar(mesa);

        // 9. Retornar DTO de respuesta
        return CerrarMesaResponse.fromDomain(mesa, pedido);
    }

    /**
     * Re-evalúa las promociones del pedido antes de cerrarlo.
     * Garantiza que el snapshot contable refleje las promociones vigentes.
     */
    private void aplicarPromocionesFinales(Pedido pedido, LocalId localId) {
        List<Promocion> promocionesActivas = promocionRepository.buscarActivasPorLocal(localId);
        
        if (promocionesActivas.isEmpty()) {
            return;
        }

        // Limpiar promociones existentes para re-evaluación completa
        pedido.limpiarPromocionesItems();

        LocalDateTime ahora = LocalDateTime.now(clock);

        // Re-aplicar promociones a cada ítem
        motorReglasService.aplicarPromociones(pedido, promocionesActivas, ahora);
    }
}
