package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.ProductoVendidoReporte;
import com.agustinpalma.comandas.application.ports.output.AnalyticsRepositoryPort;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Caso de uso de solo lectura: genera el reporte de ventas por producto.
 *
 * Delega la consulta al puerto de analytics sin reconstruir Aggregates.
 * Es una operación de analytics pura — no modifica estado del dominio.
 *
 * Resultado: lista de productos vendidos con cantidad total y monto recaudado,
 * basado exclusivamente en pedidos con estado CERRADO para la fecha indicada.
 */
@Transactional(readOnly = true)
public class ObtenerReporteVentasUseCase {

    private final AnalyticsRepositoryPort analyticsRepository;

    public ObtenerReporteVentasUseCase(AnalyticsRepositoryPort analyticsRepository) {
        this.analyticsRepository = Objects.requireNonNull(analyticsRepository,
            "El analyticsRepository es obligatorio");
    }

    /**
     * Ejecuta la consulta de ventas por producto para un día operativo.
     *
     * @param localId tenant del local
     * @param fecha   fecha operativa del reporte
     * @return lista de {@link ProductoVendidoReporte} ordenada por total descendente
     */
    public List<ProductoVendidoReporte> ejecutar(LocalId localId, LocalDate fecha) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(fecha, "La fecha del reporte es obligatoria");

        return analyticsRepository.obtenerVentasPorProducto(fecha, localId);
    }
}
