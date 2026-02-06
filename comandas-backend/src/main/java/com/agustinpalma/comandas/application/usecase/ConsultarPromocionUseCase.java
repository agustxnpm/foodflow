package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.PromocionResponse;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.PromocionId;
import com.agustinpalma.comandas.domain.model.Promocion;
import com.agustinpalma.comandas.domain.repository.PromocionRepository;

import java.util.Objects;

/**
 * Caso de uso: Consultar el detalle de una promoción específica.
 * 
 * Seguridad multi-tenant: Valida que la promoción pertenezca al LocalId solicitante.
 */
public class ConsultarPromocionUseCase {

    private final PromocionRepository promocionRepository;

    public ConsultarPromocionUseCase(PromocionRepository promocionRepository) {
        this.promocionRepository = Objects.requireNonNull(
                promocionRepository,
                "El promocionRepository es obligatorio"
        );
    }

    public PromocionResponse ejecutar(LocalId localId, PromocionId promocionId) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(promocionId, "El promocionId es obligatorio");

        Promocion promocion = promocionRepository.buscarPorIdYLocal(promocionId, localId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la promoción con id " + promocionId.getValue() +
                        " para el local " + localId.getValue()
                ));

        return PromocionResponse.fromDomain(promocion);
    }
}
