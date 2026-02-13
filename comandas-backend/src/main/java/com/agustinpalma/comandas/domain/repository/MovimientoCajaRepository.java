package com.agustinpalma.comandas.domain.repository;

import com.agustinpalma.comandas.domain.model.MovimientoCaja;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrato del repositorio de movimientos de caja.
 * Define las operaciones de persistencia sin acoplarse a tecnologías específicas.
 * La implementación concreta reside en la capa de infraestructura.
 */
public interface MovimientoCajaRepository {

    /**
     * Persiste un nuevo movimiento de caja.
     *
     * @param movimiento el movimiento a guardar
     * @return el movimiento guardado
     */
    MovimientoCaja guardar(MovimientoCaja movimiento);

    /**
     * Busca todos los movimientos de caja de un local dentro de un rango de fechas.
     * Utilizado por el reporte de caja diario para calcular egresos.
     *
     * @param localId identificador del local (tenant)
     * @param inicio inicio del rango temporal (inclusive)
     * @param fin fin del rango temporal (inclusive)
     * @return lista de movimientos en el rango
     */
    List<MovimientoCaja> buscarPorFecha(LocalId localId, LocalDateTime inicio, LocalDateTime fin);
}
