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
 * Entidad de dominio que representa el cierre de una jornada operativa de caja.
 * 
 * Una jornada NO se "abre" — se crea al momento del cierre como registro de auditoría.
 * El sistema opera sin necesidad de abrir caja explícitamente; los datos del día
 * se acumulan naturalmente a partir de pedidos cerrados y movimientos registrados.
 * 
 * La jornada guarda un snapshot contable del día que permite:
 * - Auditoría histórica (cuánto se vendió, cuánto se cobró en efectivo, etc.)
 * - Prevenir doble cierre de la misma fecha operativa
 * - Calcular fondo inicial del día siguiente (balanceEfectivo de la jornada anterior)
 * 
 * Reglas de negocio:
 * - La fecha operativa NO es necesariamente la fecha calendario del cierre.
 *   Para turnos noche (cierre después de medianoche), la fecha operativa
 *   corresponde al día anterior. Hora de corte: 06:00.
 * - Una vez creada, la jornada es inmutable.
 * - No puede existir más de una jornada cerrada por (local, fecha_operativa).
 * 
 * ⚠️ NOTA SOBRE FONDO INICIAL:
 * El fondo inicial (efectivo físico al comenzar el día) no está modelado aún.
 * Opciones futuras:
 * (a) Tomarlo del balanceEfectivo de la jornada anterior (arrastre automático)
 * (b) Permitir que el operador lo declare manualmente
 * (c) Ambos: sugerir el arrastre y permitir ajuste
 * Por ahora, el frontend maneja fondoInicial como parámetro local (default 0).
 */
public class JornadaCaja {

    /**
     * Hora de corte para determinar la fecha operativa.
     * Si el cierre ocurre ANTES de esta hora, la jornada pertenece al día anterior.
     * Justificación: ningún local gastronómico opera a las 06:00.
     */
    private static final LocalTime HORA_CORTE_JORNADA = LocalTime.of(6, 0);

    private final JornadaCajaId id;
    private final LocalId localId;
    private final LocalDate fechaOperativa;
    private final LocalDateTime fechaCierre;
    private final BigDecimal totalVentasReales;
    private final BigDecimal totalConsumoInterno;
    private final BigDecimal totalEgresos;
    private final BigDecimal balanceEfectivo;
    private final int pedidosCerradosCount;
    private final EstadoJornada estado;

    /**
     * Crea una nueva jornada de caja al momento del cierre.
     * La fecha operativa se calcula automáticamente según la hora de cierre.
     *
     * @param id identificador único de la jornada
     * @param localId identificador del local (tenant)
     * @param fechaCierre momento exacto del cierre
     * @param totalVentasReales suma de pagos comerciales (sin A_CUENTA)
     * @param totalConsumoInterno suma de pagos A_CUENTA
     * @param totalEgresos suma de egresos de caja
     * @param balanceEfectivo (pagos EFECTIVO) − egresos
     * @param pedidosCerradosCount cantidad de pedidos cerrados en la jornada
     */
    public JornadaCaja(JornadaCajaId id, LocalId localId, LocalDateTime fechaCierre,
                       BigDecimal totalVentasReales, BigDecimal totalConsumoInterno,
                       BigDecimal totalEgresos, BigDecimal balanceEfectivo,
                       int pedidosCerradosCount) {
        this.id = Objects.requireNonNull(id, "El id de la jornada no puede ser null");
        this.localId = Objects.requireNonNull(localId, "El localId no puede ser null");
        this.fechaCierre = Objects.requireNonNull(fechaCierre, "La fecha de cierre no puede ser null");
        this.totalVentasReales = Objects.requireNonNull(totalVentasReales, "totalVentasReales no puede ser null");
        this.totalConsumoInterno = Objects.requireNonNull(totalConsumoInterno, "totalConsumoInterno no puede ser null");
        this.totalEgresos = Objects.requireNonNull(totalEgresos, "totalEgresos no puede ser null");
        this.balanceEfectivo = Objects.requireNonNull(balanceEfectivo, "balanceEfectivo no puede ser null");
        this.pedidosCerradosCount = validarPedidosCount(pedidosCerradosCount);
        this.fechaOperativa = calcularFechaOperativa(fechaCierre);
        this.estado = EstadoJornada.CERRADA;
    }

    /**
     * Constructor de reconstrucción desde persistencia.
     * No recalcula la fecha operativa; usa la almacenada.
     */
    public JornadaCaja(JornadaCajaId id, LocalId localId, LocalDate fechaOperativa,
                       LocalDateTime fechaCierre, BigDecimal totalVentasReales,
                       BigDecimal totalConsumoInterno, BigDecimal totalEgresos,
                       BigDecimal balanceEfectivo, int pedidosCerradosCount,
                       EstadoJornada estado) {
        this.id = Objects.requireNonNull(id, "El id de la jornada no puede ser null");
        this.localId = Objects.requireNonNull(localId, "El localId no puede ser null");
        this.fechaOperativa = Objects.requireNonNull(fechaOperativa, "La fecha operativa no puede ser null");
        this.fechaCierre = Objects.requireNonNull(fechaCierre, "La fecha de cierre no puede ser null");
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
     * Calcula la fecha operativa a partir de la hora de cierre.
     * 
     * Regla de turno noche:
     * Si el cierre ocurre entre 00:00 y 05:59, la jornada pertenece al día anterior.
     * Esto cubre el caso de bares/restaurantes que cierran después de medianoche.
     * 
     * Ejemplo:
     * - Cierre a las 23:30 del 10/10 → fechaOperativa = 10/10
     * - Cierre a las 01:30 del 11/10 → fechaOperativa = 10/10 (turno noche)
     * - Cierre a las 06:00 del 11/10 → fechaOperativa = 11/10 (nuevo día)
     * 
     * @param fechaCierre momento exacto del cierre
     * @return fecha del día operativo (no necesariamente la fecha calendario)
     */
    public static LocalDate calcularFechaOperativa(LocalDateTime fechaCierre) {
        if (fechaCierre.toLocalTime().isBefore(HORA_CORTE_JORNADA)) {
            return fechaCierre.toLocalDate().minusDays(1);
        }
        return fechaCierre.toLocalDate();
    }

    private int validarPedidosCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("El conteo de pedidos cerrados no puede ser negativo");
        }
        return count;
    }

    // ============================================
    // Getters (inmutable, sin setters)
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

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
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
            "JornadaCaja{fechaOperativa=%s, fechaCierre=%s, ventasReales=%s, balanceEfectivo=%s, pedidos=%d}",
            fechaOperativa, fechaCierre, totalVentasReales, balanceEfectivo, pedidosCerradosCount
        );
    }
}
