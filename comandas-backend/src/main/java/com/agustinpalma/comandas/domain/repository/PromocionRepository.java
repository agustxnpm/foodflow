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

    /**
     * HU-10: Busca todas las promociones activas de un local.
     * 
     * Este método optimizado trae solo las promociones en estado ACTIVA,
     * evitando cargar basura histórica a memoria.
     * 
     * La capa de infraestructura puede optimizar esta consulta con @Query
     * para filtrar directamente en base de datos.
     * 
     * @param localId identificador del local
     * @return lista de promociones activas del local (puede estar vacía)
     */
    List<Promocion> buscarActivasPorLocal(LocalId localId);
}
