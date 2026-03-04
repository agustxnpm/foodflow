package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Value Object que representa el reporte de caja diario (arqueo).
 * 
 * NO es una entidad persistida. Se construye bajo demanda a partir de
 * los pedidos cerrados y movimientos de caja del día.
 * 
 * Inmutable y determinista: dado el mismo input, siempre produce el mismo output.
 * 
 * Definiciones:
 * - totalVentasReales: suma de pedidos cerrados excluyendo pagos A_CUENTA
 * - totalConsumoInterno: suma de pagos A_CUENTA (consumo de dueños/staff)
 * - totalIngresos: suma de ingresos manuales de efectivo (plataformas externas, etc.)
 * - totalEgresos: suma de egresos de caja (salidas de efectivo)
 * - balanceEfectivo: (total pagos EFECTIVO) + totalIngresos − totalEgresos
 * - desglosePorMedioPago: mapa con el total por cada medio de pago
 * - pedidosCerrados: lista de pedidos cerrados del día (para historial de ventas)
 */
public final class ReporteCajaDiario {

    private final BigDecimal totalVentasReales;
    private final BigDecimal totalConsumoInterno;
    private final BigDecimal totalIngresos;
    private final BigDecimal totalEgresos;
    private final BigDecimal balanceEfectivo;
    private final Map<MedioPago, BigDecimal> desglosePorMedioPago;
    private final List<MovimientoCaja> listaMovimientos;
    private final List<Pedido> pedidosCerrados;

    public ReporteCajaDiario(
            BigDecimal totalVentasReales,
            BigDecimal totalConsumoInterno,
            BigDecimal totalIngresos,
            BigDecimal totalEgresos,
            BigDecimal balanceEfectivo,
            Map<MedioPago, BigDecimal> desglosePorMedioPago,
            List<MovimientoCaja> listaMovimientos,
            List<Pedido> pedidosCerrados
    ) {
        this.totalVentasReales = Objects.requireNonNull(totalVentasReales, "totalVentasReales no puede ser null");
        this.totalConsumoInterno = Objects.requireNonNull(totalConsumoInterno, "totalConsumoInterno no puede ser null");
        this.totalIngresos = Objects.requireNonNull(totalIngresos, "totalIngresos no puede ser null");
        this.totalEgresos = Objects.requireNonNull(totalEgresos, "totalEgresos no puede ser null");
        this.balanceEfectivo = Objects.requireNonNull(balanceEfectivo, "balanceEfectivo no puede ser null");
        this.desglosePorMedioPago = Collections.unmodifiableMap(
            Objects.requireNonNull(desglosePorMedioPago, "desglosePorMedioPago no puede ser null")
        );
        this.listaMovimientos = Collections.unmodifiableList(
            Objects.requireNonNull(listaMovimientos, "listaMovimientos no puede ser null")
        );
        this.pedidosCerrados = Collections.unmodifiableList(
            Objects.requireNonNull(pedidosCerrados, "pedidosCerrados no puede ser null")
        );
    }

    public BigDecimal getTotalVentasReales() {
        return totalVentasReales;
    }

    public BigDecimal getTotalConsumoInterno() {
        return totalConsumoInterno;
    }

    public BigDecimal getTotalIngresos() {
        return totalIngresos;
    }

    public BigDecimal getTotalEgresos() {
        return totalEgresos;
    }

    public BigDecimal getBalanceEfectivo() {
        return balanceEfectivo;
    }

    public Map<MedioPago, BigDecimal> getDesglosePorMedioPago() {
        return desglosePorMedioPago;
    }

    public List<MovimientoCaja> getListaMovimientos() {
        return listaMovimientos;
    }

    public List<Pedido> getPedidosCerrados() {
        return pedidosCerrados;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReporteCajaDiario that = (ReporteCajaDiario) o;
        return totalVentasReales.compareTo(that.totalVentasReales) == 0 &&
               totalConsumoInterno.compareTo(that.totalConsumoInterno) == 0 &&
               totalIngresos.compareTo(that.totalIngresos) == 0 &&
               totalEgresos.compareTo(that.totalEgresos) == 0 &&
               balanceEfectivo.compareTo(that.balanceEfectivo) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalVentasReales, totalConsumoInterno, totalIngresos, totalEgresos, balanceEfectivo);
    }

    @Override
    public String toString() {
        return String.format(
            "ReporteCajaDiario{ventasReales=%s, consumoInterno=%s, ingresos=%s, egresos=%s, balanceEfectivo=%s}",
            totalVentasReales, totalConsumoInterno, totalIngresos, totalEgresos, balanceEfectivo
        );
    }
}
