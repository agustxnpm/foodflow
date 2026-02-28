package com.agustinpalma.comandas.domain.repository;

import com.agustinpalma.comandas.domain.model.JornadaCaja;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;

import java.time.LocalDate;
import java.util.List;

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
     * Verifica si ya existe una jornada cerrada para una fecha operativa y local.
     * Utilizado para prevenir doble cierre de la misma jornada.
     *
     * @param localId identificador del local (tenant)
     * @param fechaOperativa fecha del día operativo
     * @return true si ya existe una jornada cerrada para esa fecha
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
