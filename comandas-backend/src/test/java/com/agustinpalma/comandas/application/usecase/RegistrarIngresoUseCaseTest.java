package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.IngresoResponse;
import com.agustinpalma.comandas.domain.model.DomainEnums.TipoMovimiento;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.MovimientoCaja;
import com.agustinpalma.comandas.domain.repository.MovimientoCajaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios del caso de uso RegistrarIngresoUseCase.
 *
 * Valida:
 * - Happy path: se persiste un movimiento de tipo INGRESO con comprobante "ING-...".
 * - Monto cero: el dominio rechaza la construcción antes de persistir.
 * - No cruce de enums: el movimiento persistido es INGRESO y explícitamente NO es EGRESO.
 *
 * La distinción entre INGRESO y EGRESO en el constructor de MovimientoCaja es el contrato
 * más importante de este caso de uso — un error ahí rompería los balances de caja.
 *
 * Stack: JUnit 5 + AssertJ + Mockito.
 */
@ExtendWith(MockitoExtension.class)
class RegistrarIngresoUseCaseTest {

    @Mock
    private MovimientoCajaRepository movimientoCajaRepository;

    private RegistrarIngresoUseCase useCase;

    private LocalId localId;

    /** Reloj fijo: garantiza fecha determinista en los fixtures sin mockear java.time. */
    private static final Clock RELOJ_FIJO =
        Clock.fixed(Instant.parse("2026-04-21T12:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        useCase = new RegistrarIngresoUseCase(movimientoCajaRepository, RELOJ_FIJO);
        localId = LocalId.generate();
    }

    // ─── Tests ───────────────────────────────────────────────────────────────────

    @Test
    void deberia_registrar_ingreso_exitosamente() {
        // Given
        when(movimientoCajaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        IngresoResponse response = useCase.ejecutar(localId, BigDecimal.valueOf(2000), "Cobro PedidosYa");

        // Then — movimiento guardado de tipo INGRESO con comprobante "ING-..."
        ArgumentCaptor<MovimientoCaja> captor = ArgumentCaptor.forClass(MovimientoCaja.class);
        verify(movimientoCajaRepository, times(1)).guardar(captor.capture());

        MovimientoCaja guardado = captor.getValue();
        assertThat(guardado.getTipo()).isEqualTo(TipoMovimiento.INGRESO);
        assertThat(guardado.getMonto()).isEqualByComparingTo(BigDecimal.valueOf(2000));
        assertThat(response.tipo()).isEqualTo("INGRESO");
        assertThat(response.numeroComprobante()).startsWith("ING-");
    }

    @Test
    void deberia_rechazar_monto_cero() {
        // When / Then — la validación ocurre en el constructor de MovimientoCaja
        assertThatThrownBy(() -> useCase.ejecutar(localId, BigDecimal.ZERO, "Sin monto"))
            .isInstanceOf(IllegalArgumentException.class);

        verify(movimientoCajaRepository, never()).guardar(any());
    }

    @Test
    void deberia_crear_movimiento_tipo_ingreso() {
        // Given — el contrato crítico: el use case inyecta TipoMovimiento.INGRESO, nunca EGRESO
        when(movimientoCajaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        useCase.ejecutar(localId, BigDecimal.valueOf(500), "Ajuste manual");

        // Then — tipo es INGRESO y no es EGRESO (distinción de enum explícita)
        ArgumentCaptor<MovimientoCaja> captor = ArgumentCaptor.forClass(MovimientoCaja.class);
        verify(movimientoCajaRepository).guardar(captor.capture());

        TipoMovimiento tipo = captor.getValue().getTipo();
        assertThat(tipo).isEqualTo(TipoMovimiento.INGRESO);
        assertThat(tipo).isNotEqualTo(TipoMovimiento.EGRESO);
    }
}
