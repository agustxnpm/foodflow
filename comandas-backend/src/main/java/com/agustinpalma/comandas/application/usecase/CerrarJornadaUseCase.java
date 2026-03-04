package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.domain.exception.MesasAbiertasException;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoMesa;
import com.agustinpalma.comandas.domain.model.DomainIds.JornadaCajaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.JornadaCaja;
import com.agustinpalma.comandas.domain.model.Mesa;
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
 * 1. Busca la jornada ABIERTA del local (obligatoria)
 * 2. Valida que no existan mesas con estado ABIERTA
 * 3. Calcula el snapshot contable del día
 * 4. Transiciona la jornada de ABIERTA → CERRADA
 * 
 * Fórmula del arqueo de efectivo:
 *   balanceEfectivo = fondoInicial + ventasEFECTIVO + ingresosManuales − egresos
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
     * @return el ID de la jornada cerrada
     * @throws IllegalStateException si no hay jornada abierta
     * @throws MesasAbiertasException si existen mesas con estado ABIERTA
     */
    public JornadaCajaId ejecutar(LocalId localId) {
        Objects.requireNonNull(localId, "El localId es obligatorio");

        LocalDateTime ahora = LocalDateTime.now(clock);

        // 1. Buscar jornada ABIERTA (obligatoria para cerrar)
        JornadaCaja jornada = jornadaCajaRepository.buscarAbierta(localId)
            .orElseThrow(() -> new IllegalStateException(
                "No se puede cerrar la jornada: no hay una jornada abierta. Abra la caja primero."));

        // 2. Validar que no existan mesas abiertas
        validarMesasCerradas(localId);

        // 3. Calcular snapshot contable del día usando la fecha operativa de la jornada abierta
        LocalDate fechaOperativa = jornada.getFechaOperativa();
        SnapshotContable snapshot = calcularSnapshot(localId, fechaOperativa, jornada.getFondoInicial());

        // 4. Transicionar ABIERTA → CERRADA (el dominio valida el estado)
        jornada.cerrar(
            ahora,
            snapshot.totalVentasReales,
            snapshot.totalConsumoInterno,
            snapshot.totalEgresos,
            snapshot.balanceEfectivo,
            snapshot.pedidosCerradosCount
        );

        jornadaCajaRepository.guardar(jornada);

        return jornada.getId();
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
     * Calcula el snapshot contable del día incluyendo el fondo inicial en el arqueo.
     *
     * Fórmula: balanceEfectivo = fondoInicial + entradasEfectivo + totalIngresos − totalEgresos
     */
    private SnapshotContable calcularSnapshot(LocalId localId, LocalDate fechaOperativa,
                                               BigDecimal fondoInicial) {
        LocalDateTime inicio = fechaOperativa.atStartOfDay();
        LocalDateTime fin = fechaOperativa.atTime(LocalTime.of(23, 59, 59));

        List<Pedido> pedidosCerrados = pedidoRepository.buscarCerradosPorFecha(localId, inicio, fin);
        List<MovimientoCaja> movimientos = movimientoCajaRepository.buscarPorFecha(localId, inicio, fin);

        BigDecimal totalVentasReales = BigDecimal.ZERO;
        BigDecimal totalConsumoInterno = BigDecimal.ZERO;
        BigDecimal entradasEfectivo = BigDecimal.ZERO;

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
            }
        }

        BigDecimal totalEgresos = movimientos.stream()
            .filter(MovimientoCaja::esEgreso)
            .map(MovimientoCaja::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalIngresos = movimientos.stream()
            .filter(MovimientoCaja::esIngreso)
            .map(MovimientoCaja::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Arqueo = Fondo Inicial + Ventas Efectivo + Ingresos Manuales − Egresos
        BigDecimal balanceEfectivo = fondoInicial
            .add(entradasEfectivo)
            .add(totalIngresos)
            .subtract(totalEgresos);

        return new SnapshotContable(
            totalVentasReales, totalConsumoInterno, totalEgresos,
            balanceEfectivo, pedidosCerrados.size()
        );
    }

    /**
     * Record interno para transportar el resultado del cálculo contable.
     */
    private record SnapshotContable(
        BigDecimal totalVentasReales,
        BigDecimal totalConsumoInterno,
        BigDecimal totalEgresos,
        BigDecimal balanceEfectivo,
        int pedidosCerradosCount
    ) {}
}
