package com.agustinpalma.comandas.infrastructure.persistence.jpa;

import com.agustinpalma.comandas.infrastructure.persistence.entity.JornadaCajaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para jornadas de caja.
 * Interfaz tecnológica que delega las operaciones CRUD a Spring Data.
 */
@Repository
public interface SpringDataJornadaCajaRepository extends JpaRepository<JornadaCajaEntity, UUID> {

    /**
     * Verifica si existe una jornada cerrada para un local y fecha operativa.
     * Utilizado para prevenir doble cierre de la misma jornada.
     *
     * @param localId UUID del local
     * @param fechaOperativa fecha del día operativo
     * @return true si ya existe una jornada para esa combinación
     */
    boolean existsByLocalIdAndFechaOperativa(UUID localId, LocalDate fechaOperativa);

    /**
     * Busca jornadas por local y rango de fechas operativas.
     * Ordenadas descendente por fecha operativa (más reciente primero).
     *
     * @param localId UUID del local
     * @param desde fecha operativa inicial (inclusive)
     * @param hasta fecha operativa final (inclusive)
     * @return lista de entidades JPA ordenadas desc
     */
    List<JornadaCajaEntity> findByLocalIdAndFechaOperativaBetweenOrderByFechaOperativaDesc(
        UUID localId, LocalDate desde, LocalDate hasta
    );
}
