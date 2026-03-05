package com.agustinpalma.comandas.application.ports.output;

import com.agustinpalma.comandas.application.dto.ProductoVendidoReporte;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;

import java.time.LocalDate;
import java.util.List;

/**
 * Puerto de salida para consultas analíticas de solo lectura.
 *
 * Este puerto NO forma parte del dominio: las queries de analytics
 * son proyecciones planas sobre datos históricos (pedidos cerrados)
 * que no requieren reconstruir Aggregates.
 *
 * Al vivir en application/ports/output, respeta la arquitectura hexagonal:
 * el caso de uso depende de la abstracción, no de la implementación JPA.
 */
public interface AnalyticsRepositoryPort {

    /**
     * Obtiene el desglose de ventas agrupado por producto para una fecha operativa.
     *
     * Solo considera pedidos con estado CERRADO cuyo fecha_cierre
     * corresponda al día solicitado.
     *
     * @param fecha   fecha operativa del reporte (YYYY-MM-DD)
     * @param localId tenant del local
     * @return lista de productos con cantidad vendida y total recaudado,
     *         ordenada por total recaudado descendente
     */
    List<ProductoVendidoReporte> obtenerVentasPorProducto(LocalDate fecha, LocalId localId);
}
