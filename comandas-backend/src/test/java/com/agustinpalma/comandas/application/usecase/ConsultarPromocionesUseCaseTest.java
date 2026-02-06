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

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultarPromocionesUseCase - Tests")
class ConsultarPromocionesUseCaseTest {

    @Mock
    private PromocionRepository promocionRepository;

    private ConsultarPromocionesUseCase useCase;

    private LocalId localId;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarPromocionesUseCase(promocionRepository);
        localId = new LocalId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Debe retornar todas las promociones del local")
    void deberia_retornar_todas_las_promociones_del_local() {
        // Given
        Promocion promo1 = crearPromocionEjemplo("2x1 Cervezas", EstadoPromocion.ACTIVA);
        Promocion promo2 = crearPromocionEjemplo("20% en Pizzas", EstadoPromocion.INACTIVA);
        
        when(promocionRepository.buscarPorLocal(localId))
                .thenReturn(List.of(promo1, promo2));

        // When
        List<PromocionResponse> resultado = useCase.ejecutar(localId);

        // Then
        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(PromocionResponse::nombre)
                .containsExactlyInAnyOrder("2x1 Cervezas", "20% en Pizzas");
        
        verify(promocionRepository).buscarPorLocal(localId);
    }

    @Test
    @DisplayName("Debe filtrar promociones por estado ACTIVA")
    void deberia_filtrar_promociones_activas() {
        // Given
        Promocion activa1 = crearPromocionEjemplo("Promo Activa 1", EstadoPromocion.ACTIVA);
        Promocion activa2 = crearPromocionEjemplo("Promo Activa 2", EstadoPromocion.ACTIVA);
        Promocion inactiva = crearPromocionEjemplo("Promo Inactiva", EstadoPromocion.INACTIVA);
        
        when(promocionRepository.buscarPorLocal(localId))
                .thenReturn(List.of(activa1, activa2, inactiva));

        // When
        List<PromocionResponse> resultado = useCase.ejecutarPorEstado(localId, EstadoPromocion.ACTIVA);

        // Then
        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(PromocionResponse::estado)
                .containsOnly("ACTIVA");
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando no hay promociones")
    void deberia_retornar_lista_vacia_cuando_no_hay_promociones() {
        // Given
        when(promocionRepository.buscarPorLocal(localId))
                .thenReturn(List.of());

        // When
        List<PromocionResponse> resultado = useCase.ejecutar(localId);

        // Then
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Debe fallar si localId es null")
    void deberia_fallar_si_local_id_es_null() {
        assertThatThrownBy(() -> useCase.ejecutar(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("localId");
    }

    private Promocion crearPromocionEjemplo(String nombre, EstadoPromocion estado) {
        CriterioTemporal trigger = new CriterioTemporal(
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                Set.of(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
                LocalTime.of(18, 0),
                LocalTime.of(23, 59)
        );

        DescuentoDirecto estrategia = new DescuentoDirecto(
                ModoDescuento.PORCENTAJE,
                BigDecimal.valueOf(20)
        );

        return new Promocion(
                PromocionId.generate(),
                localId,
                nombre,
                "Descripción de ejemplo",
                10,
                estado,
                estrategia,
                List.of(trigger)
        );
    }
}
