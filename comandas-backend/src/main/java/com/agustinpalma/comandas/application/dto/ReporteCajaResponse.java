package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago;
import com.agustinpalma.comandas.domain.model.MovimientoCaja;
import com.agustinpalma.comandas.domain.model.Pago;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.model.ReporteCajaDiario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
 * @param movimientos lista de movimientos de caja del día (egresos)
 * @param ventas lista de pedidos cerrados del día (historial de ventas)
 * @param pagosDetalle lista plana de pagos individuales con contexto del pedido/mesa
 */
public record ReporteCajaResponse(
    BigDecimal totalVentasReales,
    BigDecimal totalConsumoInterno,
    BigDecimal totalEgresos,
    BigDecimal balanceEfectivo,
    Map<MedioPago, BigDecimal> desglosePorMedioPago,
    List<MovimientoResumen> movimientos,
    List<VentaResumen> ventas,
    List<PagoDetalle> pagosDetalle
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
     * Resumen de un pedido cerrado (venta) para el historial de caja.
     *
     * Contiene solo la información operativa necesaria para la UI:
     * identificación, mesa, monto total y fecha de cierre.
     *
     * @param pedidoId identificador del pedido (para operaciones como reapertura)
     * @param numeroPedido número secuencial visible del pedido
     * @param mesaNumero número de la mesa donde se consumió
     * @param total monto total cobrado (snapshot contable)
     * @param fechaCierre momento en que se cerró el pedido
     */
    public record VentaResumen(
        String pedidoId,
        int numeroPedido,
        int mesaNumero,
        BigDecimal total,
        LocalDateTime fechaCierre
    ) {
        public static VentaResumen fromDomain(Pedido pedido, Map<String, Integer> mesaNumeros) {
            String mesaIdStr = pedido.getMesaId().getValue().toString();
            int mesaNum = mesaNumeros.getOrDefault(mesaIdStr, 0);

            return new VentaResumen(
                pedido.getId().getValue().toString(),
                pedido.getNumero(),
                mesaNum,
                pedido.getMontoTotalFinal(),
                pedido.getFechaCierre()
            );
        }
    }

    /**
     * Factory method para crear la respuesta desde el Value Object de dominio.
     *
     * @param reporte reporte de caja diario del dominio
     * @param mesaNumeros mapa MesaId (string) → número de mesa
     * @return DTO de respuesta
     */
    public static ReporteCajaResponse fromDomain(ReporteCajaDiario reporte, Map<String, Integer> mesaNumeros) {
        List<MovimientoResumen> movimientos = reporte.getListaMovimientos().stream()
            .map(MovimientoResumen::fromDomain)
            .toList();

        List<VentaResumen> ventas = reporte.getPedidosCerrados().stream()
            .map(pedido -> VentaResumen.fromDomain(pedido, mesaNumeros))
            .toList();

        List<PagoDetalle> pagosDetalle = new ArrayList<>();
        for (Pedido pedido : reporte.getPedidosCerrados()) {
            String mesaIdStr = pedido.getMesaId().getValue().toString();
            int mesaNum = mesaNumeros.getOrDefault(mesaIdStr, 0);

            for (Pago pago : pedido.getPagos()) {
                pagosDetalle.add(new PagoDetalle(
                    pedido.getId().getValue().toString(),
                    pedido.getNumero(),
                    mesaNum,
                    pago.getMedio(),
                    pago.getMonto(),
                    pago.getFecha()
                ));
            }
        }

        return new ReporteCajaResponse(
            reporte.getTotalVentasReales(),
            reporte.getTotalConsumoInterno(),
            reporte.getTotalEgresos(),
            reporte.getBalanceEfectivo(),
            reporte.getDesglosePorMedioPago(),
            movimientos,
            ventas,
            pagosDetalle
        );
    }

    /**
     * Detalle de un pago individual vinculado a su pedido y mesa.
     *
     * Permite agrupar por medio de pago en la UI y navegar al pedido asociado.
     *
     * @param pedidoId identificador del pedido al que pertenece el pago
     * @param numeroPedido número secuencial visible del pedido
     * @param mesaNumero número de la mesa donde se consumió
     * @param medioPago medio de pago utilizado (EFECTIVO, TARJETA, QR, etc.)
     * @param monto monto individual de este pago
     * @param fecha momento en que se registró el pago
     */
    public record PagoDetalle(
        String pedidoId,
        int numeroPedido,
        int mesaNumero,
        MedioPago medioPago,
        BigDecimal monto,
        LocalDateTime fecha
    ) {}
}
