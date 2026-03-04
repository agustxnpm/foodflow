package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO que transporta todos los datos necesarios para generar el PDF de cierre de jornada.
 *
 * Se construye en el caso de uso a partir de la JornadaCaja y los datos detallados
 * del día (pedidos, movimientos). La capa de infraestructura (FlyingSaucer) lo consume
 * sin necesidad de conocer el dominio.
 *
 * Diseñado como record inmutable: una vez construido, no cambia.
 */
public record ReporteCierreData(

    // ── Encabezado del reporte ──────────────────────────────────────────────
    String nombreLocal,
    String direccion,
    String telefono,
    String cuit,

    // ── Identificación de la jornada ────────────────────────────────────────
    LocalDate fechaOperativa,
    LocalDateTime fechaCierre,

    // ── Resumen de arqueo ───────────────────────────────────────────────────
    BigDecimal totalVentasReales,
    BigDecimal totalConsumoInterno,
    BigDecimal totalIngresos,
    BigDecimal totalEgresos,
    BigDecimal balanceEfectivo,
    int pedidosCerradosCount,

    // ── Desglose por medio de pago ──────────────────────────────────────────
    Map<MedioPago, BigDecimal> desglosePorMedioPago,

    // ── Movimientos manuales del día ────────────────────────────────────────
    List<MovimientoDetalle> movimientos
) {

    /**
     * Detalle de un movimiento manual (egreso o ingreso) para el reporte PDF.
     */
    public record MovimientoDetalle(
        String tipo,
        BigDecimal monto,
        String descripcion,
        String numeroComprobante,
        LocalDateTime fecha
    ) {}
}
