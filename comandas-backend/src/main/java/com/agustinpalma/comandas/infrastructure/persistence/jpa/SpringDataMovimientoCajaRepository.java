package com.agustinpalma.comandas.infrastructure.persistence.jpa;

import com.agustinpalma.comandas.infrastructure.persistence.entity.MovimientoCajaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para movimientos de caja.
 * Interfaz tecnológica que delega las operaciones CRUD a Spring Data.
 */
@Repository
public interface SpringDataMovimientoCajaRepository extends JpaRepository<MovimientoCajaEntity, UUID> {

    /**
     * Busca movimientos de caja de un local en un rango de fechas.
     *
     * @param localId UUID del local
     * @param inicio inicio del rango temporal
     * @param fin fin del rango temporal
     * @return lista de movimientos en el rango
     */
    @Query("SELECT m FROM MovimientoCajaEntity m " +
           "WHERE m.localId = :localId " +
           "AND m.fecha >= :inicio " +
           "AND m.fecha <= :fin " +
           "ORDER BY m.fecha ASC")
    List<MovimientoCajaEntity> findByLocalIdAndFechaBetween(
        @Param("localId") UUID localId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );
}
