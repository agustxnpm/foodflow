package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago;
import com.agustinpalma.comandas.domain.model.MovimientoCaja;
import com.agustinpalma.comandas.domain.model.ReporteCajaDiario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO para el reporte de caja diario (arqueo).
 *
 * @param totalVentasReales suma de pedidos cerrados excluyendo pagos A_CUENTA
 * @param totalConsumoInterno suma de pagos A_CUENTA
 * @param totalEgresos suma de movimientos de caja
 * @param balanceEfectivo (total pagos EFECTIVO) − (total egresos)
 * @param desglosePorMedioPago mapa con el total por cada medio de pago comercial
 * @param movimientos lista de movimientos de caja del día
 */
public record ReporteCajaResponse(
    BigDecimal totalVentasReales,
    BigDecimal totalConsumoInterno,
    BigDecimal totalEgresos,
    BigDecimal balanceEfectivo,
    Map<MedioPago, BigDecimal> desglosePorMedioPago,
    List<MovimientoResumen> movimientos
) {

    /**
     * Resumen de un movimiento de caja para el reporte.
     */
    public record MovimientoResumen(
        String id,
        BigDecimal monto,
        String descripcion,
        LocalDateTime fecha,
        String numeroComprobante
    ) {
        public static MovimientoResumen fromDomain(MovimientoCaja movimiento) {
            return new MovimientoResumen(
                movimiento.getId().getValue().toString(),
                movimiento.getMonto(),
                movimiento.getDescripcion(),
                movimiento.getFecha(),
                movimiento.getNumeroComprobante()
            );
        }
    }

    /**
     * Factory method para crear la respuesta desde el Value Object de dominio.
     *
     * @param reporte reporte de caja diario del dominio
     * @return DTO de respuesta
     */
    public static ReporteCajaResponse fromDomain(ReporteCajaDiario reporte) {
        List<MovimientoResumen> movimientos = reporte.getListaMovimientos().stream()
            .map(MovimientoResumen::fromDomain)
            .toList();

        return new ReporteCajaResponse(
            reporte.getTotalVentasReales(),
            reporte.getTotalConsumoInterno(),
            reporte.getTotalEgresos(),
            reporte.getBalanceEfectivo(),
            reporte.getDesglosePorMedioPago(),
            movimientos
        );
    }
}
