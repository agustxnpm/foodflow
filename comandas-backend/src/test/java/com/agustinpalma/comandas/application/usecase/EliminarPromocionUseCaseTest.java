package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.domain.model.CriterioActivacion.*;
import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.EstrategiaPromocion.*;
import com.agustinpalma.comandas.domain.model.Promocion;
import com.agustinpalma.comandas.domain.repository.PromocionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EliminarPromocionUseCase - Tests")
class EliminarPromocionUseCaseTest {

    @Mock
    private PromocionRepository promocionRepository;

    private EliminarPromocionUseCase useCase;

    private LocalId localId;
    private PromocionId promocionId;

    @BeforeEach
    void setUp() {
        useCase = new EliminarPromocionUseCase(promocionRepository);
        localId = new LocalId(UUID.randomUUID());
        promocionId = PromocionId.generate();
    }

    @Test
    @DisplayName("Debe desactivar la promoción (soft delete)")
    void deberia_desactivar_promocion() {
        // Given
        Promocion promocion = crearPromocionEjemplo();
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.of(promocion));
        when(promocionRepository.guardar(any(Promocion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // When
        useCase.ejecutar(localId, promocionId);

        // Then
        ArgumentCaptor<Promocion> captor = ArgumentCaptor.forClass(Promocion.class);
        verify(promocionRepository).guardar(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoPromocion.INACTIVA);
    }

    @Test
    @DisplayName("Debe fallar si la promoción no existe o no pertenece al local")
    void deberia_fallar_si_promocion_no_existe() {
        // Given
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.ejecutar(localId, promocionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No se encontró");
    }

    @Test
    @DisplayName("Debe validar multi-tenancy al eliminar")
    void deberia_validar_multi_tenancy() {
        // Given
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.ejecutar(localId, promocionId))
                .isInstanceOf(IllegalArgumentException.class);
        
        verify(promocionRepository).buscarPorIdYLocal(promocionId, localId);
        verify(promocionRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Debe fallar si localId es null")
    void deberia_fallar_si_local_id_es_null() {
        assertThatThrownBy(() -> useCase.ejecutar(null, promocionId))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("localId");
    }

    @Test
    @DisplayName("Debe fallar si promocionId es null")
    void deberia_fallar_si_promocion_id_es_null() {
        assertThatThrownBy(() -> useCase.ejecutar(localId, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("promocionId");
    }

    @Test
    @DisplayName("Desactivar una promoción ya inactiva no debe causar error")
    void deberia_permitir_desactivar_promocion_ya_inactiva() {
        // Given
        Promocion promocionInactiva = crearPromocionEjemplo();
        promocionInactiva.desactivar(); // Ya está inactiva
        
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.of(promocionInactiva));
        when(promocionRepository.guardar(any(Promocion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // When
        useCase.ejecutar(localId, promocionId);

        // Then
        ArgumentCaptor<Promocion> captor = ArgumentCaptor.forClass(Promocion.class);
        verify(promocionRepository).guardar(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoPromocion.INACTIVA);
    }

    private Promocion crearPromocionEjemplo() {
        CriterioTemporal trigger = new CriterioTemporal(
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                Set.of(DayOfWeek.FRIDAY),
                LocalTime.of(18, 0),
                LocalTime.of(23, 59)
        );

        DescuentoDirecto estrategia = new DescuentoDirecto(
                ModoDescuento.PORCENTAJE,
                BigDecimal.valueOf(20)
        );

        return new Promocion(
                promocionId,
                localId,
                "Promoción Ejemplo",
                "Descripción",
                10,
                EstadoPromocion.ACTIVA,
                estrategia,
                List.of(trigger)
        );
    }
}
