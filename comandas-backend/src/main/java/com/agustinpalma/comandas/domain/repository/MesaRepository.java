package com.agustinpalma.comandas.domain.repository;

import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import java.util.List;

/**
 * Contrato del repositorio de mesas.
 * Define las operaciones de persistencia sin acoplarse a tecnologías específicas.
 * La implementación concreta reside en la capa de infraestructura.
 */
public interface MesaRepository {

    /**
     * Busca todas las mesas pertenecientes a un local específico.
     *
     * @param localId identificador del local (tenant)
     * @return lista de mesas del local, ordenadas por número
     */
    List<Mesa> buscarPorLocal(LocalId localId);
}
