package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.domain.exception.JornadaYaCerradaException;
import com.agustinpalma.comandas.domain.exception.MesasAbiertasException;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoMesa;
import com.agustinpalma.comandas.domain.model.DomainIds.JornadaCajaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.JornadaCaja;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.model.ReporteCajaDiario;
import com.agustinpalma.comandas.domain.model.MovimientoCaja;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.model.Pago;
import com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago;
import com.agustinpalma.comandas.domain.repository.JornadaCajaRepository;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.domain.repository.MovimientoCajaRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Caso de uso para cerrar la jornada de caja.
 * 
 * Orquesta las siguientes validaciones y acciones:
 * 1. Valida que no existan mesas con estado ABIERTA
 * 2. Verifica que no se haya cerrado ya la misma fecha operativa
 * 3. Calcula el snapshot contable del día (reutilizando la lógica del reporte)
 * 4. Persiste la jornada cerrada como registro de auditoría
 * 
 * La fecha operativa se calcula automáticamente:
 * - Si el cierre ocurre entre 00:00 y 05:59 → fechaOperativa = ayer (turno noche)
 * - Si el cierre ocurre a partir de las 06:00 → fechaOperativa = hoy
 * 
 * No contiene lógica de negocio: delega al dominio (JornadaCaja, excepciones).
 * Solo coordina repositorios y reglas ya definidas.
 */
@Transactional
public class CerrarJornadaUseCase {

    private final MesaRepository mesaRepository;
    private final PedidoRepository pedidoRepository;
    private final MovimientoCajaRepository movimientoCajaRepository;
    private final JornadaCajaRepository jornadaCajaRepository;
    private final Clock clock;

    public CerrarJornadaUseCase(MesaRepository mesaRepository,
                                 PedidoRepository pedidoRepository,
                                 MovimientoCajaRepository movimientoCajaRepository,
                                 JornadaCajaRepository jornadaCajaRepository,
                                 Clock clock) {
        this.mesaRepository = Objects.requireNonNull(mesaRepository, "mesaRepository es obligatorio");
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository, "pedidoRepository es obligatorio");
        this.movimientoCajaRepository = Objects.requireNonNull(movimientoCajaRepository, 
            "movimientoCajaRepository es obligatorio");
        this.jornadaCajaRepository = Objects.requireNonNull(jornadaCajaRepository, 
            "jornadaCajaRepository es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    /**
     * Ejecuta el cierre de jornada para un local.
     *
     * @param localId identificador del local (tenant)
     * @throws MesasAbiertasException si existen mesas con estado ABIERTA
     * @throws JornadaYaCerradaException si la jornada ya fue cerrada para la fecha operativa
     */
    public void ejecutar(LocalId localId) {
        Objects.requireNonNull(localId, "El localId es obligatorio");

        LocalDateTime ahora = LocalDateTime.now(clock);

        // 1. Validar que no existan mesas abiertas
        validarMesasCerradas(localId);

        // 2. Calcular fecha operativa (turno noche: antes de las 06:00 → día anterior)
        LocalDate fechaOperativa = JornadaCaja.calcularFechaOperativa(ahora);

        // 3. Verificar idempotencia (no cerrar dos veces el mismo día)
        if (jornadaCajaRepository.existePorFechaOperativa(localId, fechaOperativa)) {
            throw new JornadaYaCerradaException(fechaOperativa);
        }

        // 4. Calcular snapshot contable del día
        ReporteCajaDiario reporte = calcularReporte(localId, fechaOperativa);

        // 5. Crear y persistir la jornada
        JornadaCaja jornada = new JornadaCaja(
            JornadaCajaId.generate(),
            localId,
            ahora,
            reporte.getTotalVentasReales(),
            reporte.getTotalConsumoInterno(),
            reporte.getTotalEgresos(),
            reporte.getBalanceEfectivo(),
            reporte.getPedidosCerrados().size()
        );

        jornadaCajaRepository.guardar(jornada);
    }

    /**
     * Valida que todas las mesas del local estén en estado LIBRE.
     * 
     * @throws MesasAbiertasException si hay mesas con estado ABIERTA
     */
    private void validarMesasCerradas(LocalId localId) {
        List<Mesa> mesasDelLocal = mesaRepository.buscarPorLocal(localId);

        long mesasAbiertas = mesasDelLocal.stream()
            .filter(mesa -> mesa.getEstado() == EstadoMesa.ABIERTA)
            .count();

        if (mesasAbiertas > 0) {
            throw new MesasAbiertasException((int) mesasAbiertas);
        }
    }

    /**
     * Calcula el reporte de caja para la fecha operativa.
     * Replica la lógica de GenerarReporteCajaUseCase sin crear dependencia circular.
     * 
     * Alternativa considerada: inyectar GenerarReporteCajaUseCase directamente.
     * Descartada porque un use case no debería depender de otro use case — 
     * el reporte es read-only y el cierre es transaccional. Si la lógica de cálculo
     * crece, se puede extraer a un Domain Service compartido.
     */
    private ReporteCajaDiario calcularReporte(LocalId localId, LocalDate fechaOperativa) {
        LocalDateTime inicio = fechaOperativa.atStartOfDay();
        LocalDateTime fin = fechaOperativa.atTime(LocalTime.of(23, 59, 59));

        List<Pedido> pedidosCerrados = pedidoRepository.buscarCerradosPorFecha(localId, inicio, fin);
        List<MovimientoCaja> movimientos = movimientoCajaRepository.buscarPorFecha(localId, inicio, fin);

        BigDecimal totalVentasReales = BigDecimal.ZERO;
        BigDecimal totalConsumoInterno = BigDecimal.ZERO;
        BigDecimal entradasEfectivo = BigDecimal.ZERO;
        Map<MedioPago, BigDecimal> desglose = new EnumMap<>(MedioPago.class);

        for (Pedido pedido : pedidosCerrados) {
            for (Pago pago : pedido.getPagos()) {
                BigDecimal montoPago = pago.getMonto();
                MedioPago medio = pago.getMedio();

                if (medio == MedioPago.A_CUENTA) {
                    totalConsumoInterno = totalConsumoInterno.add(montoPago);
                } else {
                    totalVentasReales = totalVentasReales.add(montoPago);
                }

                if (medio == MedioPago.EFECTIVO) {
                    entradasEfectivo = entradasEfectivo.add(montoPago);
                }

                desglose.merge(medio, montoPago, BigDecimal::add);
            }
        }

        BigDecimal totalEgresos = movimientos.stream()
            .map(MovimientoCaja::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

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
