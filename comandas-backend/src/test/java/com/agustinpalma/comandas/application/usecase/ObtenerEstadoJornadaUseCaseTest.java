package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.EstadoCajaResponse;
import com.agustinpalma.comandas.domain.model.DomainIds.JornadaCajaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.JornadaCaja;
import com.agustinpalma.comandas.domain.repository.JornadaCajaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Tests del caso de uso ObtenerEstadoJornadaUseCase (Fase 6 — solo lectura).
 *
 * Es el "gatekeeper" del sistema: el frontend lo consulta para decidir
 * si mostrar la UI operativa o la pantalla de apertura.
 *
 * Escenarios:
 * - Hay jornada ABIERTA → estado=ABIERTA con datos de apertura.
 * - No hay abierta pero existe última cerrada → estado=CERRADA con saldo sugerido.
 * - No hay abierta ni jornadas previas → estado=CERRADA con saldoSugerido=null.
 *
 * Stack: JUnit 5 + AssertJ + Mockito.
 */
@ExtendWith(MockitoExtension.class)
class ObtenerEstadoJornadaUseCaseTest {

    @Mock private JornadaCajaRepository jornadaCajaRepository;

    private ObtenerEstadoJornadaUseCase useCase;
    private LocalId localId;

    @BeforeEach
    void setUp() {
        useCase = new ObtenerEstadoJornadaUseCase(jornadaCajaRepository);
        localId = LocalId.generate();
    }

    @Test
    void deberia_retornar_estado_abierta_si_hay_jornada_activa() {
        // Given — existe jornada ABIERTA con fondo $1500
        JornadaCaja jornada = new JornadaCaja(
            JornadaCajaId.generate(), localId,
            new BigDecimal("1500"),
            LocalDateTime.of(2026, 4, 21, 8, 0)
        );
        given(jornadaCajaRepository.buscarAbierta(localId)).willReturn(Optional.of(jornada));

        // When
        EstadoCajaResponse response = useCase.ejecutar(localId);

        // Then — refleja la jornada activa, sin sugerencia (no aplica)
        assertThat(response.estado()).isEqualTo("ABIERTA");
        assertThat(response.jornadaId()).isNotBlank();
        assertThat(response.fondoInicial()).isEqualByComparingTo("1500");
        assertThat(response.saldoSugerido()).isNull();

        // No se consulta la última cerrada cuando ya hay una abierta
        then(jornadaCajaRepository).should(never()).buscarUltimaCerrada(any());
    }

    @Test
    void deberia_retornar_estado_cerrada_con_saldo_sugerido_de_ultima_jornada() {
        // Given — no hay jornada abierta, pero la última cerrada dejó balance $2300
        given(jornadaCajaRepository.buscarAbierta(localId)).willReturn(Optional.empty());

        JornadaCaja ultimaCerrada = new JornadaCaja(
            JornadaCajaId.generate(), localId,
            new BigDecimal("1000"),
            LocalDateTime.of(2026, 4, 20, 8, 0)
        );
        ultimaCerrada.cerrar(
            LocalDateTime.of(2026, 4, 20, 23, 0),
            new BigDecimal("1500"),  // ventas reales
            new BigDecimal("0"),     // consumo interno
            new BigDecimal("200"),   // egresos
            new BigDecimal("2300"),  // balance efectivo final
            5                        // pedidos cerrados
        );
        given(jornadaCajaRepository.buscarUltimaCerrada(localId))
            .willReturn(Optional.of(ultimaCerrada));

        // When
        EstadoCajaResponse response = useCase.ejecutar(localId);

        // Then — el balance del último cierre se sugiere como fondo inicial
        assertThat(response.estado()).isEqualTo("CERRADA");
        assertThat(response.jornadaId()).isNull();
        assertThat(response.fondoInicial()).isNull();
        assertThat(response.saldoSugerido()).isEqualByComparingTo("2300");
    }

    @Test
    void deberia_retornar_estado_cerrada_sin_saldo_si_nunca_hubo_jornada() {
        // Given — local nuevo: sin jornada abierta y sin historial de cierres
        given(jornadaCajaRepository.buscarAbierta(localId)).willReturn(Optional.empty());
        given(jornadaCajaRepository.buscarUltimaCerrada(localId)).willReturn(Optional.empty());

        // When
        EstadoCajaResponse response = useCase.ejecutar(localId);

        // Then — estado CERRADA pero sin sugerencia (operador decide el fondo desde cero)
        assertThat(response.estado()).isEqualTo("CERRADA");
        assertThat(response.saldoSugerido()).isNull();
    }
}
