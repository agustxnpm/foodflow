package com.agustinpalma.comandas.infrastructure.persistence.jpa;

import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoJornada;
import com.agustinpalma.comandas.infrastructure.persistence.entity.JornadaCajaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para jornadas de caja.
 * Interfaz tecnológica que delega las operaciones CRUD a Spring Data.
 */
@Repository
public interface SpringDataJornadaCajaRepository extends JpaRepository<JornadaCajaEntity, UUID> {

    /**
     * Busca la jornada ABIERTA de un local (máximo una por invariante de negocio).
     */
    Optional<JornadaCajaEntity> findByLocalIdAndEstado(UUID localId, EstadoJornada estado);

    /**
     * Busca la última jornada CERRADA de un local ordenada por fecha operativa desc.
     */
    Optional<JornadaCajaEntity> findFirstByLocalIdAndEstadoOrderByFechaOperativaDesc(
        UUID localId, EstadoJornada estado
    );

    /**
     * Verifica si existe una jornada con un estado específico para un local y fecha operativa.
     */
    boolean existsByLocalIdAndFechaOperativaAndEstado(UUID localId, LocalDate fechaOperativa,
                                                      EstadoJornada estado);

    /**
     * Verifica si existe una jornada (cualquier estado) para un local y fecha operativa.
     * Utilizado para prevenir doble apertura.
     */
    boolean existsByLocalIdAndFechaOperativa(UUID localId, LocalDate fechaOperativa);

    /**
     * Busca jornadas por local y rango de fechas operativas.
     * Ordenadas descendente por fecha operativa (más reciente primero).
     */
    List<JornadaCajaEntity> findByLocalIdAndFechaOperativaBetweenOrderByFechaOperativaDesc(
        UUID localId, LocalDate desde, LocalDate hasta
    );
}
