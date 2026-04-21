package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.EgresoResponse;
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
 * Tests unitarios del caso de uso RegistrarEgresoUseCase.
 *
 * Valida:
 * - Happy path: se persiste un movimiento de tipo EGRESO con comprobante "EGR-...".
 * - Monto cero: el dominio rechaza la construcción antes de persistir.
 * - Descripción vacía: el dominio rechaza la construcción antes de persistir.
 *
 * Nota: el use case depende de Clock (inyectable) para generar la fecha del movimiento.
 * Se usa Clock.fixed para garantizar determinismo sin mockear el reloj del sistema.
 *
 * Stack: JUnit 5 + AssertJ + Mockito.
 */
@ExtendWith(MockitoExtension.class)
class RegistrarEgresoUseCaseTest {

    @Mock
    private MovimientoCajaRepository movimientoCajaRepository;

    private RegistrarEgresoUseCase useCase;

    private LocalId localId;

    /** Reloj fijo: garantiza fecha determinista en los fixtures sin mockear java.time. */
    private static final Clock RELOJ_FIJO =
        Clock.fixed(Instant.parse("2026-04-21T12:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        useCase = new RegistrarEgresoUseCase(movimientoCajaRepository, RELOJ_FIJO);
        localId = LocalId.generate();
    }

    // ─── Tests ───────────────────────────────────────────────────────────────────

    @Test
    void deberia_registrar_egreso_exitosamente() {
        // Given
        when(movimientoCajaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        EgresoResponse response = useCase.ejecutar(localId, BigDecimal.valueOf(1500), "Compra de insumos");

        // Then — el movimiento guardado debe ser de tipo EGRESO con comprobante "EGR-..."
        ArgumentCaptor<MovimientoCaja> captor = ArgumentCaptor.forClass(MovimientoCaja.class);
        verify(movimientoCajaRepository, times(1)).guardar(captor.capture());

        MovimientoCaja guardado = captor.getValue();
        assertThat(guardado.getTipo()).isEqualTo(TipoMovimiento.EGRESO);
        assertThat(guardado.getMonto()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(response.tipo()).isEqualTo("EGRESO");
        assertThat(response.numeroComprobante()).startsWith("EGR-");
    }

    @Test
    void deberia_rechazar_monto_cero() {
        // When / Then — la validación ocurre en el constructor de MovimientoCaja
        assertThatThrownBy(() -> useCase.ejecutar(localId, BigDecimal.ZERO, "Algún concepto"))
            .isInstanceOf(IllegalArgumentException.class);

        verify(movimientoCajaRepository, never()).guardar(any());
    }

    @Test
    void deberia_rechazar_descripcion_vacia() {
        // When / Then — descripción vacía rechazada antes de persistir
        assertThatThrownBy(() -> useCase.ejecutar(localId, BigDecimal.valueOf(500), ""))
            .isInstanceOf(IllegalArgumentException.class);

        verify(movimientoCajaRepository, never()).guardar(any());
    }
}
