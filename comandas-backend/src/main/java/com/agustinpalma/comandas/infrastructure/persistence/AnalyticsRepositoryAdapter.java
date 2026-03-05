package com.agustinpalma.comandas.infrastructure.persistence;

import com.agustinpalma.comandas.application.dto.ProductoVendidoReporte;
import com.agustinpalma.comandas.application.ports.output.AnalyticsRepositoryPort;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Implementación del puerto de analytics usando consultas nativas SQL.
 *
 * Se usa Native Query en lugar de JPQL porque:
 * 1. La proyección es plana (no requiere mapeo a entidades JPA)
 * 2. La agregación GROUP BY + SUM es más eficiente como query directa
 *
 * La query usa un rango de fechas (>= inicio AND < fin) en vez de DATE()
 * para ser compatible tanto con SQLite (prod) como PostgreSQL (dev).
 *
 * El resultado se mapea manualmente al DTO {@link ProductoVendidoReporte}.
 */
@Repository
@Transactional(readOnly = true)
public class AnalyticsRepositoryAdapter implements AnalyticsRepositoryPort {

    @PersistenceContext
    private final EntityManager entityManager;

    public AnalyticsRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Consulta nativa que agrupa las ventas por nombre de producto
     * para pedidos CERRADOS en la fecha indicada.
     *
     * Usa rango de timestamps (inicio del día hasta inicio del día siguiente)
     * en lugar de funciones de fecha específicas del motor (DATE(), CAST, etc.),
     * lo que garantiza compatibilidad SQLite + PostgreSQL.
     *
     * La columna subtotal se calcula como precio_unitario × cantidad
     * (refleja el método calcularSubtotal() del dominio).
     *
     * Ordenado por total recaudado descendente para mostrar primero
     * los productos de mayor ingreso.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<ProductoVendidoReporte> obtenerVentasPorProducto(LocalDate fecha, LocalId localId) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.plusDays(1).atStartOfDay();

        String sql = """
            SELECT ip.nombre_producto,
                   SUM(ip.cantidad),
                   SUM(ip.precio_unitario * ip.cantidad)
            FROM items_pedido ip
            JOIN pedidos p ON ip.pedido_id = p.id
            WHERE p.estado = 'CERRADO'
              AND p.fecha_cierre >= :inicio
              AND p.fecha_cierre < :fin
              AND p.local_id = :localId
            GROUP BY ip.nombre_producto
            ORDER BY SUM(ip.precio_unitario * ip.cantidad) DESC
            """;

        List<Object[]> rows = entityManager.createNativeQuery(sql)
            .setParameter("inicio", inicio)
            .setParameter("fin", fin)
            .setParameter("localId", localId.getValue())
            .getResultList();

        return rows.stream()
            .map(row -> new ProductoVendidoReporte(
                (String) row[0],
                ((Number) row[1]).longValue(),
                row[2] instanceof BigDecimal bd ? bd : new BigDecimal(row[2].toString())
            ))
            .toList();
    }
}
