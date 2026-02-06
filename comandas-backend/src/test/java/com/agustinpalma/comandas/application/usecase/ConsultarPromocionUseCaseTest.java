package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.PromocionResponse;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultarPromocionUseCase - Tests")
class ConsultarPromocionUseCaseTest {

    @Mock
    private PromocionRepository promocionRepository;

    private ConsultarPromocionUseCase useCase;

    private LocalId localId;
    private PromocionId promocionId;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarPromocionUseCase(promocionRepository);
        localId = new LocalId(UUID.randomUUID());
        promocionId = PromocionId.generate();
    }

    @Test
    @DisplayName("Debe retornar el detalle de la promoción cuando existe y pertenece al local")
    void deberia_retornar_detalle_promocion() {
        // Given
        Promocion promocion = crearPromocionEjemplo("2x1 Cervezas");
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.of(promocion));

        // When
        PromocionResponse resultado = useCase.ejecutar(localId, promocionId);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.nombre()).isEqualTo("2x1 Cervezas");
        verify(promocionRepository).buscarPorIdYLocal(promocionId, localId);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando la promoción no existe")
    void deberia_fallar_cuando_promocion_no_existe() {
        // Given
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.ejecutar(localId, promocionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No se encontró la promoción");
    }

    @Test
    @DisplayName("Debe validar multi-tenancy: no permite acceder a promoción de otro local")
    void deberia_validar_multi_tenancy() {
        // Given
        LocalId otroLocalId = new LocalId(UUID.randomUUID());
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.ejecutar(localId, promocionId))
                .isInstanceOf(IllegalArgumentException.class);
        
        verify(promocionRepository).buscarPorIdYLocal(promocionId, localId);
        verify(promocionRepository, never()).buscarPorIdYLocal(promocionId, otroLocalId);
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

    private Promocion crearPromocionEjemplo(String nombre) {
        CriterioTemporal trigger = new CriterioTemporal(
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                Set.of(DayOfWeek.FRIDAY),
                LocalTime.of(18, 0),
                LocalTime.of(23, 59)
        );

        CantidadFija estrategia = new CantidadFija(2, 1);

        return new Promocion(
                promocionId,
                localId,
                nombre,
                "Descripción de ejemplo",
                10,
                EstadoPromocion.ACTIVA,
                estrategia,
                List.of(trigger)
        );
    }
}
