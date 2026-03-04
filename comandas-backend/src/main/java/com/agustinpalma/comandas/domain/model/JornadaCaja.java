package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoJornada;
import com.agustinpalma.comandas.domain.model.DomainIds.JornadaCajaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Aggregate Root que representa una jornada operativa de caja.
 *
 * Ciclo de vida: ABIERTA → CERRADA (irreversible).
 *
 * Una jornada se ABRE explícitamente declarando el fondo inicial (efectivo
 * físico en el cajón). Durante el día el sistema opera normalmente. Al
 * finalizar la jornada, se CIERRA con un snapshot contable.
 *
 * Reglas de negocio:
 * - Solo puede existir una jornada ABIERTA por local a la vez.
 * - La fecha operativa se calcula al momento de la apertura usando la
 *   hora de corte (06:00) para turnos noche.
 * - El fondo inicial se declara al abrir y no puede modificarse después.
 * - La combinación (local_id, fecha_operativa) es única.
 * - Una vez CERRADA, la jornada es inmutable.
 *
 * Fórmula de arqueo:
 *   Arqueo = Fondo Inicial + Ventas Efectivo + Ingresos Manuales − Egresos Manuales
 */
public class JornadaCaja {

    /**
     * Hora de corte para determinar la fecha operativa.
     * Si la apertura/cierre ocurre ANTES de esta hora, la jornada pertenece
     * al día anterior. Ningún local gastronómico opera a las 06:00.
     */
    private static final LocalTime HORA_CORTE_JORNADA = LocalTime.of(6, 0);

    private final JornadaCajaId id;
    private final LocalId localId;
    private final LocalDate fechaOperativa;
    private final LocalDateTime fechaApertura;
    private final BigDecimal fondoInicial;

    // Campos de snapshot — se completan al cerrar
    private LocalDateTime fechaCierre;
    private BigDecimal totalVentasReales;
    private BigDecimal totalConsumoInterno;
    private BigDecimal totalEgresos;
    private BigDecimal balanceEfectivo;
    private int pedidosCerradosCount;
    private EstadoJornada estado;

    // ============================================
    // Constructores
    // ============================================

    /**
     * Constructor de apertura: crea una jornada ABIERTA con fondo inicial.
     *
     * La fecha operativa se calcula automáticamente según la hora de apertura.
     * Los campos de snapshot se inicializan en cero; se completan al cerrar.
     *
     * @param id            identificador único de la jornada
     * @param localId       identificador del local (tenant)
     * @param fondoInicial  efectivo físico declarado al abrir (≥ 0)
     * @param fechaApertura momento exacto de la apertura
     */
    public JornadaCaja(JornadaCajaId id, LocalId localId,
                       BigDecimal fondoInicial, LocalDateTime fechaApertura) {
        this.id = Objects.requireNonNull(id, "El id de la jornada no puede ser null");
        this.localId = Objects.requireNonNull(localId, "El localId no puede ser null");
        this.fondoInicial = validarFondoInicial(fondoInicial);
        this.fechaApertura = Objects.requireNonNull(fechaApertura, "La fecha de apertura no puede ser null");
        this.fechaOperativa = calcularFechaOperativa(fechaApertura);
        this.estado = EstadoJornada.ABIERTA;

        // Snapshot vacío — se completa al cerrar
        this.fechaCierre = null;
        this.totalVentasReales = BigDecimal.ZERO;
        this.totalConsumoInterno = BigDecimal.ZERO;
        this.totalEgresos = BigDecimal.ZERO;
        this.balanceEfectivo = fondoInicial;
        this.pedidosCerradosCount = 0;
    }

    /**
     * Constructor de reconstrucción desde persistencia.
     * No recalcula la fecha operativa; usa la almacenada.
     * Acepta campos nullables para compatibilidad con jornadas históricas.
     */
    public JornadaCaja(JornadaCajaId id, LocalId localId, LocalDate fechaOperativa,
                       LocalDateTime fechaApertura, LocalDateTime fechaCierre,
                       BigDecimal fondoInicial,
                       BigDecimal totalVentasReales, BigDecimal totalConsumoInterno,
                       BigDecimal totalEgresos, BigDecimal balanceEfectivo,
                       int pedidosCerradosCount, EstadoJornada estado) {
        this.id = Objects.requireNonNull(id, "El id de la jornada no puede ser null");
        this.localId = Objects.requireNonNull(localId, "El localId no puede ser null");
        this.fechaOperativa = Objects.requireNonNull(fechaOperativa, "La fecha operativa no puede ser null");
        this.fechaApertura = fechaApertura; // nullable para jornadas históricas pre-apertura
        this.fechaCierre = fechaCierre;     // nullable para jornadas ABIERTA
        this.fondoInicial = Objects.requireNonNull(fondoInicial, "El fondo inicial no puede ser null");
        this.totalVentasReales = Objects.requireNonNull(totalVentasReales, "totalVentasReales no puede ser null");
        this.totalConsumoInterno = Objects.requireNonNull(totalConsumoInterno, "totalConsumoInterno no puede ser null");
        this.totalEgresos = Objects.requireNonNull(totalEgresos, "totalEgresos no puede ser null");
        this.balanceEfectivo = Objects.requireNonNull(balanceEfectivo, "balanceEfectivo no puede ser null");
        this.pedidosCerradosCount = pedidosCerradosCount;
        this.estado = Objects.requireNonNull(estado, "El estado no puede ser null");
    }

    // ============================================
    // Lógica de dominio
    // ============================================

    /**
     * Cierra la jornada con el snapshot contable del día.
     *
     * Transición de estado: ABIERTA → CERRADA.
     * Una vez cerrada, la jornada es inmutable.
     *
     * @param fechaCierre          momento exacto del cierre
     * @param totalVentasReales    suma de pagos comerciales (sin A_CUENTA)
     * @param totalConsumoInterno  suma de pagos A_CUENTA
     * @param totalEgresos         suma de egresos de caja
     * @param balanceEfectivo      fondo + efectivo + ingresos − egresos
     * @param pedidosCerradosCount cantidad de pedidos cerrados en la jornada
     * @throws IllegalStateException si la jornada no está ABIERTA
     */
    public void cerrar(LocalDateTime fechaCierre,
                       BigDecimal totalVentasReales, BigDecimal totalConsumoInterno,
                       BigDecimal totalEgresos, BigDecimal balanceEfectivo,
                       int pedidosCerradosCount) {
        if (this.estado != EstadoJornada.ABIERTA) {
            throw new IllegalStateException(
                "No se puede cerrar una jornada que no está ABIERTA. Estado actual: " + this.estado
            );
        }
        this.fechaCierre = Objects.requireNonNull(fechaCierre, "La fecha de cierre no puede ser null");
        this.totalVentasReales = Objects.requireNonNull(totalVentasReales);
        this.totalConsumoInterno = Objects.requireNonNull(totalConsumoInterno);
        this.totalEgresos = Objects.requireNonNull(totalEgresos);
        this.balanceEfectivo = Objects.requireNonNull(balanceEfectivo);
        this.pedidosCerradosCount = validarPedidosCount(pedidosCerradosCount);
        this.estado = EstadoJornada.CERRADA;
    }

    /**
     * Indica si la jornada está abierta y permite operaciones.
     */
    public boolean estaAbierta() {
        return this.estado == EstadoJornada.ABIERTA;
    }

    /**
     * Calcula la fecha operativa a partir de un timestamp.
     *
     * Regla de turno noche:
     * Si el momento es entre 00:00 y 05:59, la jornada pertenece al día anterior.
     *
     * Ejemplo:
     * - 23:30 del 10/10 → fechaOperativa = 10/10
     * - 01:30 del 11/10 → fechaOperativa = 10/10 (turno noche)
     * - 06:00 del 11/10 → fechaOperativa = 11/10 (nuevo día)
     *
     * @param momento timestamp a evaluar
     * @return fecha del día operativo
     */
    public static LocalDate calcularFechaOperativa(LocalDateTime momento) {
        if (momento.toLocalTime().isBefore(HORA_CORTE_JORNADA)) {
            return momento.toLocalDate().minusDays(1);
        }
        return momento.toLocalDate();
    }

    private BigDecimal validarFondoInicial(BigDecimal fondoInicial) {
        Objects.requireNonNull(fondoInicial, "El fondo inicial no puede ser null");
        if (fondoInicial.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El fondo inicial no puede ser negativo");
        }
        return fondoInicial;
    }

    private int validarPedidosCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("El conteo de pedidos cerrados no puede ser negativo");
        }
        return count;
    }

    // ============================================
    // Getters
    // ============================================

    public JornadaCajaId getId() {
        return id;
    }

    public LocalId getLocalId() {
        return localId;
    }

    public LocalDate getFechaOperativa() {
        return fechaOperativa;
    }

    public LocalDateTime getFechaApertura() {
        return fechaApertura;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public BigDecimal getFondoInicial() {
        return fondoInicial;
    }

    public BigDecimal getTotalVentasReales() {
        return totalVentasReales;
    }

    public BigDecimal getTotalConsumoInterno() {
        return totalConsumoInterno;
    }

    public BigDecimal getTotalEgresos() {
        return totalEgresos;
    }

    public BigDecimal getBalanceEfectivo() {
        return balanceEfectivo;
    }

    public int getPedidosCerradosCount() {
        return pedidosCerradosCount;
    }

    public EstadoJornada getEstado() {
        return estado;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JornadaCaja that = (JornadaCaja) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
            "JornadaCaja{estado=%s, fechaOperativa=%s, fondoInicial=%s, balanceEfectivo=%s, pedidos=%d}",
            estado, fechaOperativa, fondoInicial, balanceEfectivo, pedidosCerradosCount
        );
    }
}
