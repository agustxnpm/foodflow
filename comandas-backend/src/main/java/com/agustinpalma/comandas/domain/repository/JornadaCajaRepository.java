package com.agustinpalma.comandas.domain.repository;

import com.agustinpalma.comandas.domain.model.JornadaCaja;
import com.agustinpalma.comandas.domain.model.DomainIds.JornadaCajaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Contrato del repositorio de jornadas de caja.
 * Define las operaciones de persistencia sin acoplarse a tecnologías específicas.
 * La implementación concreta reside en la capa de infraestructura.
 */
public interface JornadaCajaRepository {

    /**
     * Persiste una nueva jornada de caja (cierre diario).
     *
     * @param jornada la jornada a guardar
     * @return la jornada guardada
     */
    JornadaCaja guardar(JornadaCaja jornada);

    /**
     * Busca una jornada de caja por su identificador.
     *
     * @param id identificador de la jornada
     * @return la jornada si existe, vacío si no
     */
    Optional<JornadaCaja> buscarPorId(JornadaCajaId id);

    /**
     * Busca la jornada ABIERTA activa para un local.
     * Debería haber como máximo una jornada ABIERTA por local (invariante de negocio).
     *
     * @param localId identificador del local (tenant)
     * @return la jornada abierta si existe, vacío si no
     */
    Optional<JornadaCaja> buscarAbierta(LocalId localId);

    /**
     * Busca la última jornada CERRADA de un local (la más reciente por fecha operativa).
     * Utilizado para sugerir el saldo remanente como fondo inicial del día siguiente.
     *
     * @param localId identificador del local (tenant)
     * @return la última jornada cerrada si existe, vacío si nunca hubo cierre
     */
    Optional<JornadaCaja> buscarUltimaCerrada(LocalId localId);

    /**
     * Verifica si ya existe una jornada cerrada para una fecha operativa y local.
     * Utilizado para prevenir doble cierre de la misma jornada.
     *
     * @param localId identificador del local (tenant)
     * @param fechaOperativa fecha del día operativo
     * @return true si ya existe una jornada cerrada para esa fecha
     */
    boolean existeCerradaPorFechaOperativa(LocalId localId, LocalDate fechaOperativa);

    /**
     * Verifica si ya existe una jornada (cualquier estado) para una fecha operativa y local.
     * Utilizado para prevenir doble apertura de la misma fecha operativa.
     *
     * @param localId identificador del local (tenant)
     * @param fechaOperativa fecha del día operativo
     * @return true si ya existe una jornada para esa fecha
     */
    boolean existePorFechaOperativa(LocalId localId, LocalDate fechaOperativa);

    /**
     * Busca las jornadas cerradas de un local dentro de un rango de fechas operativas.
     * Ordenadas por fecha operativa descendente (más reciente primero).
     *
     * @param localId identificador del local (tenant)
     * @param desde fecha operativa inicial (inclusive)
     * @param hasta fecha operativa final (inclusive)
     * @return lista de jornadas en el rango, ordenadas desc por fecha operativa
     */
    List<JornadaCaja> buscarPorRangoFecha(LocalId localId, LocalDate desde, LocalDate hasta);
}
