package com.agustinpalma.comandas.infrastructure.persistence.jpa;

import com.agustinpalma.comandas.infrastructure.persistence.entity.MesaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para mesas.
 * Interfaz tecnológica que delega las operaciones CRUD a Spring Data.
 */
@Repository
public interface SpringDataMesaRepository extends JpaRepository<MesaEntity, UUID> {

    /**
     * Busca todas las mesas de un local específico, ordenadas por número.
     *
     * @param localId UUID del local
     * @return lista de entidades JPA ordenadas
     */
    @Query("SELECT m FROM MesaEntity m WHERE m.localId = :localId ORDER BY m.numero ASC")
    List<MesaEntity> findByLocalIdOrderByNumeroAsc(@Param("localId") UUID localId);
}
