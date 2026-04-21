package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.AbrirCajaResponse;
import com.agustinpalma.comandas.domain.exception.JornadaYaAbiertaException;
import com.agustinpalma.comandas.domain.exception.JornadaYaCerradaException;
import com.agustinpalma.comandas.domain.model.DomainIds.JornadaCajaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.JornadaCaja;
import com.agustinpalma.comandas.domain.repository.JornadaCajaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Tests del caso de uso AbrirJornadaUseCase.
 *
 * Escenarios:
 * - Happy path: apertura con fondo válido → persiste y retorna response.
 * - Guard: jornada ya abierta → lanza JornadaYaAbiertaException.
 * - Turno noche: Clock fijo a las 02:00 AM → fechaOperativa = día anterior.
 *
 * Stack: JUnit 5 + AssertJ + Mockito (MockitoExtension).
 * Sin Spring. Sin base de datos.
 */
@ExtendWith(MockitoExtension.class)
class AbrirJornadaUseCaseTest {

    @Mock
    private JornadaCajaRepository jornadaCajaRepository;

    // Use case instanciado manualmente en @BeforeEach para poder inyectar Clock.fixed().
    private AbrirJornadaUseCase useCase;

    private LocalId localId;

    // Clock por defecto: 08:00 AM — jornada diurna normal (fechaOperativa = mismo día)
    private static final Clock CLOCK_DIURNO = Clock.fixed(
        LocalDateTime.of(2026, 4, 21, 8, 0).toInstant(ZoneOffset.UTC),
        ZoneOffset.UTC
    );

    @BeforeEach
    void setUp() {
        localId = LocalId.generate();
        useCase = new AbrirJornadaUseCase(jornadaCajaRepository, CLOCK_DIURNO);
    }

    // ============================================================
    // Happy path
    // ============================================================

    @Nested
    class AperturaExitosa {

        @Test
        void deberia_abrir_jornada_exitosamente() {
            // Given — no existe jornada abierta ni previa para esta fecha
            given(jornadaCajaRepository.buscarAbierta(localId)).willReturn(Optional.empty());
            given(jornadaCajaRepository.existePorFechaOperativa(eq(localId), any()))
                .willReturn(false);

            // When
            AbrirCajaResponse response = useCase.ejecutar(localId, new BigDecimal("1000"));

            // Then — respuesta con datos correctos
            assertThat(response.jornadaId()).isNotBlank();
            assertThat(response.fondoInicial()).isEqualByComparingTo("1000");
            assertThat(response.fechaOperativa()).isEqualTo("2026-04-21");

            // Verificar que la jornada fue persistida exactamente una vez
            then(jornadaCajaRepository).should(times(1)).guardar(any(JornadaCaja.class));
        }
    }

    // ============================================================
    // Guard: jornada ya abierta
    // ============================================================

    @Nested
    class GuardJornadaPrevia {

        @Test
        void deberia_rechazar_si_ya_hay_jornada_abierta() {
            // Given — ya existe una jornada ABIERTA en el sistema
            JornadaCaja jornadaExistente = new JornadaCaja(
                JornadaCajaId.generate(),
                localId,
                new BigDecimal("500"),
                LocalDateTime.of(2026, 4, 21, 7, 0)
            );
            given(jornadaCajaRepository.buscarAbierta(localId))
                .willReturn(Optional.of(jornadaExistente));

            // When / Then — el use case rechaza la doble apertura
            assertThatThrownBy(() ->
                useCase.ejecutar(localId, new BigDecimal("1000"))
            ).isInstanceOf(JornadaYaAbiertaException.class);

            // Verificar que no se persistió nada
            then(jornadaCajaRepository).should(never()).guardar(any());
        }
    }

    // ============================================================
    // Lógica de turno noche via Clock inyectado
    // ============================================================

    @Nested
    class TurnoNoche {

        @Test
        void deberia_calcular_fecha_operativa_automaticamente() {
            // Given — Clock fijo a las 02:00 AM: momento de turno noche
            Clock clockNocturno = Clock.fixed(
                LocalDateTime.of(2026, 4, 21, 2, 0).toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC
            );
            AbrirJornadaUseCase useCaseNocturno =
                new AbrirJornadaUseCase(jornadaCajaRepository, clockNocturno);

            given(jornadaCajaRepository.buscarAbierta(localId)).willReturn(Optional.empty());
            // La fechaOperativa calculada debe ser 2026-04-20 (día anterior)
            given(jornadaCajaRepository.existePorFechaOperativa(
                eq(localId),
                eq(java.time.LocalDate.of(2026, 4, 20))
            )).willReturn(false);

            // When
            AbrirCajaResponse response = useCaseNocturno.ejecutar(localId, new BigDecimal("500"));

            // Then — las 02:00 AM del 21/04 pertenecen a la jornada del 20/04
            assertThat(response.fechaOperativa()).isEqualTo("2026-04-20");
        }
    }

    // ============================================================
    // Guard: jornada ya cerrada para la misma fecha operativa (R7 ampliado)
    // ============================================================

    @Nested
    class GuardFechaOperativaDuplicada {

        @Test
        void deberia_rechazar_si_existe_jornada_cerrada_misma_fecha() {
            // Given — no hay jornada ABIERTA, pero la fecha operativa de hoy ya tiene
            //         una jornada cerrada (no se puede reabrir el día contable)
            given(jornadaCajaRepository.buscarAbierta(localId)).willReturn(Optional.empty());
            given(jornadaCajaRepository.existePorFechaOperativa(
                eq(localId),
                eq(java.time.LocalDate.of(2026, 4, 21))
            )).willReturn(true);

            // When / Then — el use case bloquea la doble apertura del mismo día contable
            assertThatThrownBy(() ->
                useCase.ejecutar(localId, new BigDecimal("1000"))
            ).isInstanceOf(JornadaYaCerradaException.class);

            // No se persistió ninguna jornada nueva
            then(jornadaCajaRepository).should(never()).guardar(any());
        }
    }
}
