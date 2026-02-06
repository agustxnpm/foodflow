package com.agustinpalma.comandas.domain.repository;

import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.PromocionId;
import com.agustinpalma.comandas.domain.model.Promocion;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de repositorio para el aggregate Promocion.
 * Definido en el dominio, implementado en infraestructura.
 */
public interface PromocionRepository {

    Promocion guardar(Promocion promocion);

    Optional<Promocion> buscarPorId(PromocionId id);

    Optional<Promocion> buscarPorIdYLocal(PromocionId id, LocalId localId);

    boolean existePorNombreYLocal(String nombre, LocalId localId);

    List<Promocion> buscarPorLocal(LocalId localId);
}
