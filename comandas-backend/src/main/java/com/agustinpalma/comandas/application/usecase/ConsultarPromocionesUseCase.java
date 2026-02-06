package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.PromocionResponse;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPromocion;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.Promocion;
import com.agustinpalma.comandas.domain.repository.PromocionRepository;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Caso de uso: Consultar todas las promociones de un local.
 * 
 * Seguridad multi-tenant: Solo retorna promociones del LocalId especificado.
 * Puede filtrar opcionalmente por estado (ACTIVA/INACTIVA).
 */
public class ConsultarPromocionesUseCase {

    private final PromocionRepository promocionRepository;

    public ConsultarPromocionesUseCase(PromocionRepository promocionRepository) {
        this.promocionRepository = Objects.requireNonNull(
                promocionRepository,
                "El promocionRepository es obligatorio"
        );
    }

    /**
     * Retorna todas las promociones del local.
     */
    public List<PromocionResponse> ejecutar(LocalId localId) {
        Objects.requireNonNull(localId, "El localId es obligatorio");

        List<Promocion> promociones = promocionRepository.buscarPorLocal(localId);

        return promociones.stream()
                .map(PromocionResponse::fromDomain)
                .collect(Collectors.toList());
    }

    /**
     * Retorna las promociones del local filtradas por estado.
     */
    public List<PromocionResponse> ejecutarPorEstado(LocalId localId, EstadoPromocion estado) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(estado, "El estado es obligatorio");

        List<Promocion> promociones = promocionRepository.buscarPorLocal(localId);

        return promociones.stream()
                .filter(p -> p.getEstado() == estado)
                .map(PromocionResponse::fromDomain)
                .collect(Collectors.toList());
    }
}
