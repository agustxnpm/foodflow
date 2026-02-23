package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.PromocionResponse;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPromocion;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.PromocionId;
import com.agustinpalma.comandas.domain.model.Promocion;
import com.agustinpalma.comandas.domain.repository.PromocionRepository;

import java.util.Objects;

/**
 * Caso de uso para cambiar el estado (ACTIVA/INACTIVA) de una promoción.
 *
 * Responsabilidad:
 * - Recibe el estado deseado y lo aplica al aggregate.
 * - Delega la decisión al dominio (activar/desactivar).
 * - No contiene lógica de negocio.
 *
 * Decisión de diseño: Se separa de EditarPromocionUseCase porque
 * cambiar el estado es una operación semántica distinta a editar
 * nombre/descripción/triggers. Además, el frontend necesita un
 * endpoint liviano para el toggle sin reenviar todos los campos.
 */
public class CambiarEstadoPromocionUseCase {

    private final PromocionRepository promocionRepository;

    public CambiarEstadoPromocionUseCase(PromocionRepository promocionRepository) {
        this.promocionRepository = Objects.requireNonNull(
                promocionRepository,
                "El promocionRepository es obligatorio"
        );
    }

    public PromocionResponse ejecutar(LocalId localId, PromocionId promocionId, EstadoPromocion nuevoEstado) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(promocionId, "El promocionId es obligatorio");
        Objects.requireNonNull(nuevoEstado, "El nuevoEstado es obligatorio");

        Promocion promocion = promocionRepository.buscarPorIdYLocal(promocionId, localId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la promoción con id " + promocionId.getValue() +
                        " para el local " + localId.getValue()
                ));

        // Delegar al dominio: activar() o desactivar()
        if (nuevoEstado == EstadoPromocion.ACTIVA) {
            promocion.activar();
        } else {
            promocion.desactivar();
        }

        Promocion actualizada = promocionRepository.guardar(promocion);
        return PromocionResponse.fromDomain(actualizada);
    }
}
