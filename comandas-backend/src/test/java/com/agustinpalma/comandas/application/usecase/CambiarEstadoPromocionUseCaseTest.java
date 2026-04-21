package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.PromocionResponse;
import com.agustinpalma.comandas.domain.model.CriterioActivacion.CriterioTemporal;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPromocion;
import com.agustinpalma.comandas.domain.model.DomainEnums.ModoDescuento;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.PromocionId;
import com.agustinpalma.comandas.domain.model.EstrategiaPromocion.DescuentoDirecto;
import com.agustinpalma.comandas.domain.model.Promocion;
import com.agustinpalma.comandas.domain.repository.PromocionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del caso de uso CambiarEstadoPromocionUseCase.
 *
 * Escenarios:
 * - Activar una promoción que estaba INACTIVA.
 * - Desactivar una promoción que estaba ACTIVA.
 * - Multi-tenancy: `buscarPorIdYLocal` rechaza automáticamente
 *   cualquier promoción que no pertenezca al tenant activo.
 *
 * Stack: JUnit 5 + AssertJ + Mockito.
 */
@ExtendWith(MockitoExtension.class)
class CambiarEstadoPromocionUseCaseTest {

    @Mock
    private PromocionRepository promocionRepository;

    private CambiarEstadoPromocionUseCase useCase;

    private LocalId localId;
    private PromocionId promocionId;

    @BeforeEach
    void setUp() {
        useCase = new CambiarEstadoPromocionUseCase(promocionRepository);
        localId = LocalId.generate();
        promocionId = PromocionId.generate();
    }

    // ============================================================
    // Helpers
    // ============================================================

    private Promocion crearPromocionConEstado(EstadoPromocion estado) {
        CriterioTemporal trigger = new CriterioTemporal(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Set.of(DayOfWeek.FRIDAY),
                LocalTime.of(18, 0),
                LocalTime.of(23, 59)
        );
        DescuentoDirecto estrategia = new DescuentoDirecto(ModoDescuento.PORCENTAJE, BigDecimal.valueOf(15));
        return new Promocion(
                promocionId,
                localId,
                "Promo Viernes",
                "15% los viernes",
                1,
                estado,
                estrategia,
                List.of(trigger)
        );
    }

    // ============================================================
    // Activar promoción inactiva
    // ============================================================

    @Test
    void deberia_activar_promocion_inactiva() {
        // Given — la promoción está INACTIVA
        Promocion inactiva = crearPromocionConEstado(EstadoPromocion.INACTIVA);
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.of(inactiva));
        when(promocionRepository.guardar(any(Promocion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // When
        PromocionResponse response = useCase.ejecutar(localId, promocionId, EstadoPromocion.ACTIVA);

        // Then — el estado guardado debe ser ACTIVA
        ArgumentCaptor<Promocion> captor = ArgumentCaptor.forClass(Promocion.class);
        verify(promocionRepository).guardar(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoPromocion.ACTIVA);
        assertThat(response).isNotNull();
    }

    // ============================================================
    // Desactivar promoción activa
    // ============================================================

    @Test
    void deberia_desactivar_promocion_activa() {
        // Given — la promoción está ACTIVA
        Promocion activa = crearPromocionConEstado(EstadoPromocion.ACTIVA);
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.of(activa));
        when(promocionRepository.guardar(any(Promocion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // When
        PromocionResponse response = useCase.ejecutar(localId, promocionId, EstadoPromocion.INACTIVA);

        // Then — el estado guardado debe ser INACTIVA
        ArgumentCaptor<Promocion> captor = ArgumentCaptor.forClass(Promocion.class);
        verify(promocionRepository).guardar(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoPromocion.INACTIVA);
        assertThat(response).isNotNull();
    }

    // ============================================================
    // Multi-tenancy: promoción de otro local
    // ============================================================

    @Test
    void deberia_rechazar_promocion_de_otro_local() {
        // Given — buscarPorIdYLocal devuelve empty porque la promoción
        //         no pertenece al tenant activo (aislamiento multi-tenant)
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.empty());

        // When / Then — el use case lanza excepción al no encontrar la promoción para ese local
        assertThatThrownBy(() -> useCase.ejecutar(localId, promocionId, EstadoPromocion.ACTIVA))
                .isInstanceOf(IllegalArgumentException.class);

        // El repositorio nunca debe haber persistido nada de otro tenant
        verify(promocionRepository, never()).guardar(any());
    }
}
