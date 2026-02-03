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

    /**
     * Verifica si existe una mesa con el número dado en un local específico.
     *
     * @param numero número de la mesa
     * @param localId UUID del local
     * @return true si existe, false si no
     */
    @Query("SELECT COUNT(m) > 0 FROM MesaEntity m WHERE m.numero = :numero AND m.localId = :localId")
    boolean existsByNumeroAndLocalId(@Param("numero") int numero, @Param("localId") UUID localId);

    /**
     * Cuenta cuántas mesas tiene un local específico.
     *
     * @param localId UUID del local
     * @return cantidad de mesas
     */
    @Query("SELECT COUNT(m) FROM MesaEntity m WHERE m.localId = :localId")
    long countByLocalId(@Param("localId") UUID localId);
}
