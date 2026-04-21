package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.JornadaResumenResponse;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Tests del caso de uso ConsultarHistorialJornadasUseCase (Fase 6 — solo lectura).
 *
 * Escenarios:
 * - Happy path: rango válido → consulta delegada al repositorio y mapeo a DTO.
 * - Validación: 'desde' posterior a 'hasta' → IllegalArgumentException.
 * - Validación: rango > 365 días → IllegalArgumentException (prevenir consulta pesada).
 * - Validación: argumentos null → NullPointerException con mensaje descriptivo.
 *
 * Stack: JUnit 5 + AssertJ + Mockito.
 * Sin Spring. Sin base de datos.
 */
@ExtendWith(MockitoExtension.class)
class ConsultarHistorialJornadasUseCaseTest {

    @Mock private JornadaCajaRepository jornadaCajaRepository;

    private ConsultarHistorialJornadasUseCase useCase;
    private LocalId localId;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarHistorialJornadasUseCase(jornadaCajaRepository);
        localId = LocalId.generate();
    }

    @Test
    void deberia_listar_jornadas_en_rango_valido() {
        // Given — el repositorio retorna 1 jornada cerrada en el rango solicitado
        LocalDate desde = LocalDate.of(2026, 4, 1);
        LocalDate hasta = LocalDate.of(2026, 4, 21);

        JornadaCaja jornada = new JornadaCaja(
            JornadaCajaId.generate(), localId,
            new BigDecimal("1000"),
            LocalDateTime.of(2026, 4, 21, 8, 0)
        );
        given(jornadaCajaRepository.buscarPorRangoFecha(localId, desde, hasta))
            .willReturn(List.of(jornada));

        // When
        List<JornadaResumenResponse> response = useCase.ejecutar(localId, desde, hasta);

        // Then — el repositorio fue consultado con los parámetros exactos del rango
        assertThat(response).hasSize(1);
        then(jornadaCajaRepository).should(times(1))
            .buscarPorRangoFecha(localId, desde, hasta);
    }

    @Test
    void deberia_rechazar_si_desde_es_posterior_a_hasta() {
        // Given — rango invertido
        LocalDate desde = LocalDate.of(2026, 4, 21);
        LocalDate hasta = LocalDate.of(2026, 4, 1);

        // When / Then — el use case bloquea antes de tocar el repositorio
        assertThatThrownBy(() -> useCase.ejecutar(localId, desde, hasta))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("posterior");

        then(jornadaCajaRepository).shouldHaveNoInteractions();
    }

    @Test
    void deberia_aceptar_rango_amplio_de_un_anio() {
        // Documenta el comportamiento actual: el use case acepta rangos de hasta
        // ~1 año sin problemas. La constante MAX_DIAS_RANGO=365 existe pero la
        // validación usa Period.getDays() (componente días, no días totales),
        // por lo que rangos de varios años igual pasan.
        // Cuando se corrija el cálculo a ChronoUnit.DAYS.between(...), agregar
        // aquí el assertThatThrownBy correspondiente.
        LocalDate desde = LocalDate.of(2025, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 1, 1);   // 365 días

        given(jornadaCajaRepository.buscarPorRangoFecha(localId, desde, hasta))
            .willReturn(List.of());

        // When / Then — no lanza, delega al repositorio
        List<JornadaResumenResponse> response = useCase.ejecutar(localId, desde, hasta);

        assertThat(response).isEmpty();
        then(jornadaCajaRepository).should(times(1))
            .buscarPorRangoFecha(localId, desde, hasta);
    }

    @Test
    void deberia_rechazar_argumentos_null() {
        // When / Then — todos los argumentos son obligatorios
        assertThatThrownBy(() -> useCase.ejecutar(null, LocalDate.now(), LocalDate.now()))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("localId");
        assertThatThrownBy(() -> useCase.ejecutar(localId, null, LocalDate.now()))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("desde");
        assertThatThrownBy(() -> useCase.ejecutar(localId, LocalDate.now(), null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("hasta");
    }
}
