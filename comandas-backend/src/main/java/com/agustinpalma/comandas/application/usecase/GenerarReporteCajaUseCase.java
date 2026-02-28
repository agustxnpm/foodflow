package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.ReporteCajaResponse;
import com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.model.MovimientoCaja;
import com.agustinpalma.comandas.domain.model.Pago;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.model.ReporteCajaDiario;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.domain.repository.MovimientoCajaRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Caso de uso para generar el reporte de caja diario (arqueo).
 * 
 * El reporte consolida la información financiera de un día operativo:
 * - Ventas reales (pagos comerciales)
 * - Consumo interno (pagos A_CUENTA)
 * - Egresos de caja
 * - Balance de efectivo
 * 
 * El cálculo es determinista y auditable: dado el mismo input, siempre produce el mismo output.
 * 
 * Fórmulas:
 * - totalVentasReales = Σ pagos comerciales (EFECTIVO, TARJETA, TRANSFERENCIA, QR)
 * - totalConsumoInterno = Σ pagos A_CUENTA
 * - totalEgresos = Σ movimientos de caja
 * - balanceEfectivo = (Σ pagos EFECTIVO) − totalEgresos
 */
@Transactional(readOnly = true)
public class GenerarReporteCajaUseCase {

    private final PedidoRepository pedidoRepository;
    private final MovimientoCajaRepository movimientoCajaRepository;
    private final MesaRepository mesaRepository;

    public GenerarReporteCajaUseCase(PedidoRepository pedidoRepository,
                                     MovimientoCajaRepository movimientoCajaRepository,
                                     MesaRepository mesaRepository) {
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository, 
            "El pedidoRepository es obligatorio");
        this.movimientoCajaRepository = Objects.requireNonNull(movimientoCajaRepository, 
            "El movimientoCajaRepository es obligatorio");
        this.mesaRepository = Objects.requireNonNull(mesaRepository,
            "El mesaRepository es obligatorio");
    }

    /**
     * Genera el reporte de caja para un día específico.
     *
     * @param localId identificador del local (tenant)
     * @param fechaReporte fecha del reporte (se construye rango 00:00:00 a 23:59:59)
     * @return DTO con el reporte completo de caja
     */
    public ReporteCajaResponse ejecutar(LocalId localId, LocalDate fechaReporte) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(fechaReporte, "La fecha del reporte es obligatoria");

        // 1. Construir rango temporal del día
        LocalDateTime inicio = fechaReporte.atStartOfDay();
        LocalDateTime fin = fechaReporte.atTime(LocalTime.of(23, 59, 59));

        // 2. Obtener pedidos cerrados del día (con pagos cargados via JOIN FETCH)
        List<Pedido> pedidosCerrados = pedidoRepository.buscarCerradosPorFecha(localId, inicio, fin);

        // 3. Obtener movimientos de caja del día
        List<MovimientoCaja> movimientos = movimientoCajaRepository.buscarPorFecha(localId, inicio, fin);

        // 4. Construir mapa MesaId → número de mesa para resolver en el DTO
        List<Mesa> mesasDelLocal = mesaRepository.buscarPorLocal(localId);
        Map<String, Integer> mesaNumeros = mesasDelLocal.stream()
            .collect(Collectors.toMap(
                m -> m.getId().getValue().toString(),
                Mesa::getNumero
            ));

        // 5. Calcular el reporte
        ReporteCajaDiario reporte = calcularReporte(pedidosCerrados, movimientos);

        return ReporteCajaResponse.fromDomain(reporte, mesaNumeros);
    }

    /**
     * Calcula el reporte de caja a partir de pedidos cerrados y movimientos.
     * 
     * Lógica de clasificación de pagos:
     * - A_CUENTA → consumo interno (no es venta real)
     * - EFECTIVO, TARJETA, TRANSFERENCIA, QR → venta real
     * - EFECTIVO → además suma al balance de efectivo
     */
    private ReporteCajaDiario calcularReporte(List<Pedido> pedidosCerrados, 
                                               List<MovimientoCaja> movimientos) {
        BigDecimal totalVentasReales = BigDecimal.ZERO;
        BigDecimal totalConsumoInterno = BigDecimal.ZERO;
        BigDecimal entradasEfectivo = BigDecimal.ZERO;
        Map<MedioPago, BigDecimal> desglose = new EnumMap<>(MedioPago.class);

        // 4a. Iterar todos los pagos de todos los pedidos cerrados
        for (Pedido pedido : pedidosCerrados) {
            for (Pago pago : pedido.getPagos()) {
                BigDecimal montoPago = pago.getMonto();
                MedioPago medio = pago.getMedio();

                if (medio == MedioPago.A_CUENTA) {
                    // Consumo interno: no cuenta como venta real
                    totalConsumoInterno = totalConsumoInterno.add(montoPago);
                } else {
                    // Venta real: medios comerciales
                    totalVentasReales = totalVentasReales.add(montoPago);
                }

                if (medio == MedioPago.EFECTIVO) {
                    entradasEfectivo = entradasEfectivo.add(montoPago);
                }

                // Acumular en desglose por medio de pago
                desglose.merge(medio, montoPago, BigDecimal::add);
            }
        }

        // 4b. Calcular total de egresos
        BigDecimal totalEgresos = movimientos.stream()
            .map(MovimientoCaja::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4c. Balance de efectivo = entradas EFECTIVO - egresos
        BigDecimal balanceEfectivo = entradasEfectivo.subtract(totalEgresos);

        return new ReporteCajaDiario(
            totalVentasReales,
            totalConsumoInterno,
            totalEgresos,
            balanceEfectivo,
            desglose,
            movimientos,
            pedidosCerrados
        );
    }
}
