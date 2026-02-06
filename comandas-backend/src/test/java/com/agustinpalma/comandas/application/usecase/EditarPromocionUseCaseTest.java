package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.CrearPromocionCommand.TriggerParams;
import com.agustinpalma.comandas.application.dto.EditarPromocionCommand;
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
@DisplayName("EditarPromocionUseCase - Tests")
class EditarPromocionUseCaseTest {

    @Mock
    private PromocionRepository promocionRepository;

    private EditarPromocionUseCase useCase;

    private LocalId localId;
    private PromocionId promocionId;

    @BeforeEach
    void setUp() {
        useCase = new EditarPromocionUseCase(promocionRepository);
        localId = new LocalId(UUID.randomUUID());
        promocionId = PromocionId.generate();
    }

    @Test
    @DisplayName("Debe actualizar el nombre de la promoción")
    void deberia_actualizar_nombre() {
        // Given
        Promocion promocionOriginal = crearPromocionEjemplo("Nombre Original");
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.of(promocionOriginal));
        when(promocionRepository.existePorNombreYLocal("Nuevo Nombre", localId))
                .thenReturn(false);
        when(promocionRepository.guardar(any(Promocion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TriggerParams trigger = new TriggerParams(
                "TEMPORAL",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                Set.of(DayOfWeek.FRIDAY),
                LocalTime.of(18, 0),
                LocalTime.of(23, 59),
                null,
                null
        );

        EditarPromocionCommand command = new EditarPromocionCommand(
                "Nuevo Nombre",
                "Nueva descripción",
                15,
                List.of(trigger)
        );

        // When
        PromocionResponse resultado = useCase.ejecutar(localId, promocionId, command);

        // Then
        assertThat(resultado.nombre()).isEqualTo("Nuevo Nombre");
        
        ArgumentCaptor<Promocion> captor = ArgumentCaptor.forClass(Promocion.class);
        verify(promocionRepository).guardar(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Nuevo Nombre");
        assertThat(captor.getValue().getDescripcion()).isEqualTo("Nueva descripción");
        assertThat(captor.getValue().getPrioridad()).isEqualTo(15);
    }

    @Test
    @DisplayName("Debe validar unicidad del nombre si cambió")
    void deberia_validar_unicidad_nombre() {
        // Given
        Promocion promocionOriginal = crearPromocionEjemplo("Nombre Original");
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.of(promocionOriginal));
        when(promocionRepository.existePorNombreYLocal("Nombre Existente", localId))
                .thenReturn(true);

        EditarPromocionCommand command = new EditarPromocionCommand(
                "Nombre Existente",
                null,
                null,
                null
        );

        // When & Then
        assertThatThrownBy(() -> useCase.ejecutar(localId, promocionId, command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe una promoción");
    }

    @Test
    @DisplayName("No debe validar unicidad si el nombre no cambió")
    void no_deberia_validar_unicidad_si_nombre_no_cambio() {
        // Given
        Promocion promocionOriginal = crearPromocionEjemplo("Mismo Nombre");
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.of(promocionOriginal));
        when(promocionRepository.guardar(any(Promocion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TriggerParams trigger = new TriggerParams(
                "TEMPORAL",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                null,
                null,
                null,
                null,
                null
        );

        EditarPromocionCommand command = new EditarPromocionCommand(
                "Mismo Nombre",
                "Nueva descripción",
                10,
                List.of(trigger)
        );

        // When
        useCase.ejecutar(localId, promocionId, command);

        // Then
        verify(promocionRepository, never()).existePorNombreYLocal(anyString(), any());
    }

    @Test
    @DisplayName("Debe actualizar los triggers cuando se especifican")
    void deberia_actualizar_triggers() {
        // Given
        Promocion promocionOriginal = crearPromocionEjemplo("Promoción");
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.of(promocionOriginal));
        when(promocionRepository.guardar(any(Promocion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UUID productoId = UUID.randomUUID();
        TriggerParams triggerContenido = new TriggerParams(
                "CONTENIDO",
                null,
                null,
                null,
                null,
                null,
                List.of(productoId.toString()),
                null
        );

        TriggerParams triggerMonto = new TriggerParams(
                "MONTO_MINIMO",
                null,
                null,
                null,
                null,
                null,
                null,
                BigDecimal.valueOf(1000)
        );

        EditarPromocionCommand command = new EditarPromocionCommand(
                "Promoción",
                null,
                null,
                List.of(triggerContenido, triggerMonto)
        );

        // When
        useCase.ejecutar(localId, promocionId, command);

        // Then
        ArgumentCaptor<Promocion> captor = ArgumentCaptor.forClass(Promocion.class);
        verify(promocionRepository).guardar(captor.capture());
        assertThat(captor.getValue().getTriggers()).hasSize(2);
        assertThat(captor.getValue().getTriggers().get(0)).isInstanceOf(CriterioContenido.class);
        assertThat(captor.getValue().getTriggers().get(1)).isInstanceOf(CriterioMontoMinimo.class);
    }

    @Test
    @DisplayName("Debe fallar si la promoción no existe o no pertenece al local")
    void deberia_fallar_si_promocion_no_existe() {
        // Given
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.empty());

        EditarPromocionCommand command = new EditarPromocionCommand(
                "Nuevo Nombre",
                null,
                null,
                null
        );

        // When & Then
        assertThatThrownBy(() -> useCase.ejecutar(localId, promocionId, command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No se encontró");
    }

    private Promocion crearPromocionEjemplo(String nombre) {
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
                nombre,
                "Descripción original",
                10,
                EstadoPromocion.ACTIVA,
                estrategia,
                List.of(trigger)
        );
    }
}
