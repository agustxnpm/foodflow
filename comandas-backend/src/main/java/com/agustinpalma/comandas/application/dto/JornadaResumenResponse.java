package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.JornadaCaja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para el historial de jornadas cerradas.
 *
 * Cada registro representa un cierre de caja diario (snapshot contable).
 * Se usa en la pantalla de consulta histórica para:
 *   - Lista expandible con resumen financiero
 *   - Gráfico interactivo por rango de fechas
 *
 * @param id                    UUID de la jornada
 * @param fechaOperativa        Fecha del día operativo (puede diferir del calendario si es turno noche)
 * @param fechaCierre           Timestamp exacto del momento de cierre
 * @param totalVentasReales     Suma de cobros reales (excluye A_CUENTA)
 * @param totalConsumoInterno   Suma de pagos A_CUENTA (consumo empleados)
 * @param totalEgresos          Suma de salidas de efectivo del día
 * @param balanceEfectivo       Entradas EFECTIVO − Egresos
 * @param pedidosCerradosCount  Cantidad de pedidos cerrados en la jornada
 */
public record JornadaResumenResponse(
    String id,
    LocalDate fechaOperativa,
    LocalDateTime fechaCierre,
    BigDecimal totalVentasReales,
    BigDecimal totalConsumoInterno,
    BigDecimal totalEgresos,
    BigDecimal balanceEfectivo,
    int pedidosCerradosCount
) {

    /**
     * Factory method que construye el DTO desde la entidad de dominio.
     * Centraliza el mapeo para evitar acoplamiento directo en el use case.
     */
    public static JornadaResumenResponse fromDomain(JornadaCaja jornada) {
        return new JornadaResumenResponse(
            jornada.getId().getValue().toString(),
            jornada.getFechaOperativa(),
            jornada.getFechaCierre(),
            jornada.getTotalVentasReales(),
            jornada.getTotalConsumoInterno(),
            jornada.getTotalEgresos(),
            jornada.getBalanceEfectivo(),
            jornada.getPedidosCerradosCount()
        );
    }
}
