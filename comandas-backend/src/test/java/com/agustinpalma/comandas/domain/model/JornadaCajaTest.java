package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoJornada;
import com.agustinpalma.comandas.domain.model.DomainIds.JornadaCajaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de dominio puro para JornadaCaja (Aggregate Root del ciclo contable).
 *
 * Escenarios:
 * - Ciclo de vida: apertura → cierre (irreversible)
 * - Lógica de turno noche: momentos antes, en y después de las 06:00
 *   determinan a qué fecha operativa pertenece la jornada.
 *
 * Sin Spring. Sin base de datos. Sin mocks.
 */
class JornadaCajaTest {

    private static final LocalDateTime APERTURA_MANANA  = LocalDateTime.of(2026, 4, 21,  8,  0);
    private static final LocalDateTime CIERRE_TARDE      = LocalDateTime.of(2026, 4, 21, 23, 30);

    private LocalId localId;

    @BeforeEach
    void setUp() {
        localId = LocalId.generate();
    }

    // ============================================================
    // 1. Apertura y validaciones de construcción
    // ============================================================

    @Nested
    class Apertura {

        @Test
        void deberia_crear_jornada_abierta_con_fondo_valido() {
            // Given / When
            JornadaCaja jornada = new JornadaCaja(
                JornadaCajaId.generate(),
                localId,
                new BigDecimal("1500"),
                APERTURA_MANANA
            );

            // Then — estado inicial y campos
            assertThat(jornada.getEstado()).isEqualTo(EstadoJornada.ABIERTA);
            assertThat(jornada.estaAbierta()).isTrue();
            assertThat(jornada.getFondoInicial()).isEqualByComparingTo("1500");
            assertThat(jornada.getFechaApertura()).isEqualTo(APERTURA_MANANA);
            assertThat(jornada.getFechaCierre()).isNull();

            // Los acumulados de ventas/egresos comienzan en cero; el balance refleja el fondo inicial
            assertThat(jornada.getTotalVentasReales()).isEqualByComparingTo("0");
            assertThat(jornada.getTotalConsumoInterno()).isEqualByComparingTo("0");
            assertThat(jornada.getTotalEgresos()).isEqualByComparingTo("0");
            assertThat(jornada.getBalanceEfectivo()).isEqualByComparingTo("1500");
            assertThat(jornada.getPedidosCerradosCount()).isZero();
        }

        @Test
        void deberia_rechazar_fondo_negativo() {
            // When / Then — fondoInicial < 0 viola la invariante del dominio
            assertThatThrownBy(() ->
                new JornadaCaja(
                    JornadaCajaId.generate(),
                    localId,
                    new BigDecimal("-1"),
                    APERTURA_MANANA
                )
            ).isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================================
    // 2. Cierre de jornada
    // ============================================================

    @Nested
    class Cierre {

        @Test
        void deberia_cerrar_jornada_abierta() {
            // Given
            JornadaCaja jornada = new JornadaCaja(
                JornadaCajaId.generate(),
                localId,
                new BigDecimal("500"),
                APERTURA_MANANA
            );

            // When
            jornada.cerrar(
                CIERRE_TARDE,
                new BigDecimal("2000"),  // totalVentasReales
                new BigDecimal("100"),   // totalConsumoInterno
                new BigDecimal("50"),    // totalEgresos
                new BigDecimal("2450"),  // balanceEfectivo
                5                        // pedidosCerradosCount
            );

            // Then — transición irreversible a CERRADA con snapshot congelado
            assertThat(jornada.getEstado()).isEqualTo(EstadoJornada.CERRADA);
            assertThat(jornada.estaAbierta()).isFalse();
            assertThat(jornada.getFechaCierre()).isEqualTo(CIERRE_TARDE);
            assertThat(jornada.getTotalVentasReales()).isEqualByComparingTo("2000");
            assertThat(jornada.getTotalConsumoInterno()).isEqualByComparingTo("100");
            assertThat(jornada.getTotalEgresos()).isEqualByComparingTo("50");
            assertThat(jornada.getBalanceEfectivo()).isEqualByComparingTo("2450");
            assertThat(jornada.getPedidosCerradosCount()).isEqualTo(5);
        }

        @Test
        void deberia_rechazar_cerrar_jornada_ya_cerrada() {
            // Given — jornada ya cerrada
            JornadaCaja jornada = new JornadaCaja(
                JornadaCajaId.generate(),
                localId,
                new BigDecimal("500"),
                APERTURA_MANANA
            );
            jornada.cerrar(CIERRE_TARDE, BigDecimal.ZERO, BigDecimal.ZERO,
                           BigDecimal.ZERO, new BigDecimal("500"), 0);

            // When / Then — el ciclo ABIERTA→CERRADA es irreversible
            assertThatThrownBy(() ->
                jornada.cerrar(CIERRE_TARDE.plusHours(1), BigDecimal.ZERO, BigDecimal.ZERO,
                               BigDecimal.ZERO, new BigDecimal("500"), 0)
            ).isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    // 3. Lógica de turno noche — calcularFechaOperativa
    //
    //    Regla: momentos ANTES de las 06:00 pertenecen al día ANTERIOR
    //    (ej.: el mozo cierra la jornada del 20/04 a las 03:00 del 21/04)
    // ============================================================

    @Nested
    class FechaOperativaTurnoNoche {

        @Test
        void deberia_calcular_fecha_operativa_antes_de_las_6() {
            // Given — 03:00 AM del 21 de abril → turno noche del 20/04
            LocalDateTime momentoNocturno = LocalDateTime.of(2026, 4, 21, 3, 0);

            // When
            LocalDate fechaOperativa = JornadaCaja.calcularFechaOperativa(momentoNocturno);

            // Then — asignada al día anterior
            assertThat(fechaOperativa).isEqualTo(LocalDate.of(2026, 4, 20));
        }

        @Test
        void deberia_calcular_fecha_operativa_despues_de_las_6() {
            // Given — 08:00 AM del 21 de abril → jornada normal del 21/04
            LocalDateTime momentoDiurno = LocalDateTime.of(2026, 4, 21, 8, 0);

            // When
            LocalDate fechaOperativa = JornadaCaja.calcularFechaOperativa(momentoDiurno);

            // Then — asignada al mismo día
            assertThat(fechaOperativa).isEqualTo(LocalDate.of(2026, 4, 21));
        }

        @Test
        void deberia_calcular_fecha_operativa_exactamente_a_las_6() {
            // Given — 06:00:00 exacto → límite del corte nocturno
            LocalDateTime momentoExacto = LocalDateTime.of(2026, 4, 21, 6, 0);

            // When
            LocalDate fechaOperativa = JornadaCaja.calcularFechaOperativa(momentoExacto);

            // Then — a las 06:00 exactas el turno nocturno terminó: asignada al mismo día
            assertThat(fechaOperativa).isEqualTo(LocalDate.of(2026, 4, 21));
        }
    }
}
