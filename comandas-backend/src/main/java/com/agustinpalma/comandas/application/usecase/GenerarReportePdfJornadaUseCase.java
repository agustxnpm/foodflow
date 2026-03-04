package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.ReporteCierreData;
import com.agustinpalma.comandas.application.ports.output.ReportePdfGenerator;
import com.agustinpalma.comandas.domain.exception.JornadaNoEncontradaException;
import com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago;
import com.agustinpalma.comandas.domain.model.DomainIds.JornadaCajaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.JornadaCaja;
import com.agustinpalma.comandas.domain.model.MovimientoCaja;
import com.agustinpalma.comandas.domain.model.Pago;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.repository.JornadaCajaRepository;
import com.agustinpalma.comandas.domain.repository.MovimientoCajaRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import com.agustinpalma.comandas.infrastructure.config.MeisenProperties;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Caso de uso para generar el PDF del reporte de cierre de jornada.
 *
 * Orquestación:
 * 1. Busca la jornada por ID (validando existencia)
 * 2. Recalcula el desglose detallado a partir de pedidos y movimientos del día
 *    (la jornada solo guarda totales; el PDF necesita el desglose completo)
 * 3. Construye el DTO de datos del reporte con info del local
 * 4. Delega la generación de bytes PDF al puerto ReportePdfGenerator
 *
 * No contiene lógica de presentación ni de infraestructura.
 */
@Transactional(readOnly = true)
public class GenerarReportePdfJornadaUseCase {

    private final JornadaCajaRepository jornadaCajaRepository;
    private final PedidoRepository pedidoRepository;
    private final MovimientoCajaRepository movimientoCajaRepository;
    private final ReportePdfGenerator reportePdfGenerator;
    private final MeisenProperties meisenProperties;

    public GenerarReportePdfJornadaUseCase(
            JornadaCajaRepository jornadaCajaRepository,
            PedidoRepository pedidoRepository,
            MovimientoCajaRepository movimientoCajaRepository,
            ReportePdfGenerator reportePdfGenerator,
            MeisenProperties meisenProperties) {
        this.jornadaCajaRepository = Objects.requireNonNull(jornadaCajaRepository);
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository);
        this.movimientoCajaRepository = Objects.requireNonNull(movimientoCajaRepository);
        this.reportePdfGenerator = Objects.requireNonNull(reportePdfGenerator);
        this.meisenProperties = Objects.requireNonNull(meisenProperties);
    }

    /**
     * Genera el PDF del reporte de cierre para una jornada existente.
     *
     * @param jornadaId identificador de la jornada cerrada
     * @param localId   identificador del local (validación de tenant)
     * @return bytes del PDF generado
     * @throws JornadaNoEncontradaException si la jornada no existe
     */
    public byte[] ejecutar(JornadaCajaId jornadaId, LocalId localId) {
        Objects.requireNonNull(jornadaId, "El jornadaId es obligatorio");
        Objects.requireNonNull(localId, "El localId es obligatorio");

        // 1. Buscar la jornada
        JornadaCaja jornada = jornadaCajaRepository.buscarPorId(jornadaId)
            .orElseThrow(() -> new JornadaNoEncontradaException(jornadaId));

        // 2. Recalcular desglose detallado del día
        LocalDate fechaOperativa = jornada.getFechaOperativa();
        LocalDateTime inicio = fechaOperativa.atStartOfDay();
        LocalDateTime fin = fechaOperativa.atTime(LocalTime.of(23, 59, 59));

        List<Pedido> pedidosCerrados = pedidoRepository.buscarCerradosPorFecha(localId, inicio, fin);
        List<MovimientoCaja> movimientos = movimientoCajaRepository.buscarPorFecha(localId, inicio, fin);

        Map<MedioPago, BigDecimal> desglose = calcularDesglosePorMedioPago(pedidosCerrados);
        BigDecimal totalIngresos = calcularTotalIngresos(movimientos);

        List<ReporteCierreData.MovimientoDetalle> movimientoDetalles = movimientos.stream()
            .map(m -> new ReporteCierreData.MovimientoDetalle(
                m.getTipo().name(),
                m.getMonto(),
                m.getDescripcion(),
                m.getNumeroComprobante(),
                m.getFecha()
            ))
            .toList();

        // 3. Construir DTO con datos del local
        MeisenProperties.LocalProperties localProps = meisenProperties.getLocal();

        ReporteCierreData data = new ReporteCierreData(
            localProps.getNombreLocal(),
            localProps.getDireccion(),
            localProps.getTelefono(),
            localProps.getCuit(),
            jornada.getFechaOperativa(),
            jornada.getFechaCierre(),
            jornada.getTotalVentasReales(),
            jornada.getTotalConsumoInterno(),
            totalIngresos,
            jornada.getTotalEgresos(),
            jornada.getBalanceEfectivo(),
            jornada.getPedidosCerradosCount(),
            desglose,
            movimientoDetalles
        );

        // 4. Generar PDF
        return reportePdfGenerator.generarReporteCierre(data);
    }

    private Map<MedioPago, BigDecimal> calcularDesglosePorMedioPago(List<Pedido> pedidos) {
        Map<MedioPago, BigDecimal> desglose = new EnumMap<>(MedioPago.class);
        for (Pedido pedido : pedidos) {
            for (Pago pago : pedido.getPagos()) {
                desglose.merge(pago.getMedio(), pago.getMonto(), BigDecimal::add);
            }
        }
        return desglose;
    }

    private BigDecimal calcularTotalIngresos(List<MovimientoCaja> movimientos) {
        return movimientos.stream()
            .filter(MovimientoCaja::esIngreso)
            .map(MovimientoCaja::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
