package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.CrearPromocionCommand;
import com.agustinpalma.comandas.application.dto.CrearPromocionCommand.*;
import com.agustinpalma.comandas.application.dto.PromocionResponse;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.Promocion;
import com.agustinpalma.comandas.domain.repository.PromocionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del caso de uso CrearPromocionUseCase.
 * Utiliza mocks para aislar la lógica del caso de uso.
 * 
 * Escenarios probados:
 * - Creación exitosa de promo tipo DESCUENTO_DIRECTO (porcentaje)
 * - Creación exitosa de promo tipo DESCUENTO_DIRECTO (monto fijo)
 * - Creación exitosa de promo tipo CANTIDAD_FIJA (2x1)
 * - Creación exitosa de promo tipo COMBO_CONDICIONAL
 * - Creación con múltiples triggers (temporal + contenido)
 * - Rechazo por nombre duplicado en el mismo local
 * - Rechazo por prioridad negativa
 * - Rechazo por tipo de estrategia inválido
 * - Rechazo por parámetros faltantes según tipo
 */
@ExtendWith(MockitoExtension.class)
class CrearPromocionUseCaseTest {

    @Mock
    private PromocionRepository promocionRepository;

    private CrearPromocionUseCase useCase;

    private LocalId localId;

    @BeforeEach
    void setUp() {
        useCase = new CrearPromocionUseCase(promocionRepository);
        localId = LocalId.generate();
    }

    // ============================================
    // Escenarios exitosos
    // ============================================

    @Test
    void deberia_crear_promocion_tipo_descuento_porcentaje() {
        // Given
        var triggerTemporal = new TriggerParams(
                "TEMPORAL",
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 3, 1),
                null, null, null,
                null, null
        );

        var command = new CrearPromocionCommand(
                "20% en empanadas",
                "Promo de verano",
                1,
                "DESCUENTO_DIRECTO",
                new DescuentoDirectoParams("PORCENTAJE", new BigDecimal("20")),
                null, null,
                List.of(triggerTemporal)
        );

        when(promocionRepository.existePorNombreYLocal("20% en empanadas", localId))
                .thenReturn(false);
        when(promocionRepository.guardar(any(Promocion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PromocionResponse response = useCase.ejecutar(localId, command);

        // Then
        assertNotNull(response);
        assertEquals("20% en empanadas", response.nombre());
        assertEquals("Promo de verano", response.descripcion());
        assertEquals(1, response.prioridad());
        assertEquals("ACTIVA", response.estado());
        assertEquals("DESCUENTO_DIRECTO", response.estrategia().tipo());
        assertEquals("PORCENTAJE", response.estrategia().modoDescuento());
        assertEquals(new BigDecimal("20"), response.estrategia().valorDescuento());
        assertEquals(1, response.triggers().size());
        assertEquals("TEMPORAL", response.triggers().get(0).tipo());

        verify(promocionRepository).existePorNombreYLocal("20% en empanadas", localId);
        verify(promocionRepository).guardar(any(Promocion.class));
    }

    @Test
    void deberia_crear_promocion_tipo_descuento_monto_fijo() {
        // Given
        var triggerTemporal = new TriggerParams(
                "TEMPORAL",
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 28),
                null, null, null,
                null, null
        );

        var command = new CrearPromocionCommand(
                "$500 off en pizza",
                null,
                3,
                "DESCUENTO_DIRECTO",
                new DescuentoDirectoParams("MONTO_FIJO", new BigDecimal("500")),
                null, null,
                List.of(triggerTemporal)
        );

        when(promocionRepository.existePorNombreYLocal("$500 off en pizza", localId))
                .thenReturn(false);
        when(promocionRepository.guardar(any(Promocion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PromocionResponse response = useCase.ejecutar(localId, command);

        // Then
        assertNotNull(response);
        assertEquals("DESCUENTO_DIRECTO", response.estrategia().tipo());
        assertEquals("MONTO_FIJO", response.estrategia().modoDescuento());
        assertEquals(new BigDecimal("500"), response.estrategia().valorDescuento());
    }

    @Test
    void deberia_crear_promocion_tipo_cantidad_fija_2x1() {
        // Given
        var triggerTemporal = new TriggerParams(
                "TEMPORAL",
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 12, 31),
                EnumSet.of(DayOfWeek.TUESDAY),
                LocalTime.of(18, 0),
                LocalTime.of(23, 59),
                null, null
        );

        var command = new CrearPromocionCommand(
                "2x1 en cervezas",
                "Martes de birra",
                5,
                "CANTIDAD_FIJA",
                null,
                new CantidadFijaParams(2, 1),
                null,
                List.of(triggerTemporal)
        );

        when(promocionRepository.existePorNombreYLocal("2x1 en cervezas", localId))
                .thenReturn(false);
        when(promocionRepository.guardar(any(Promocion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PromocionResponse response = useCase.ejecutar(localId, command);

        // Then
        assertNotNull(response);
        assertEquals("CANTIDAD_FIJA", response.estrategia().tipo());
        assertEquals(2, response.estrategia().cantidadLlevas());
        assertEquals(1, response.estrategia().cantidadPagas());
        assertNull(response.estrategia().modoDescuento());

        // Validar trigger temporal
        assertEquals(1, response.triggers().size());
        var trigger = response.triggers().get(0);
        assertEquals("TEMPORAL", trigger.tipo());
        assertTrue(trigger.diasSemana().contains(DayOfWeek.TUESDAY));
    }

    @Test
    void deberia_crear_promocion_tipo_combo_condicional() {
        // Given - "Si comprás 1 hamburguesa, la gaseosa tiene 50% off"
        var triggerTemporal = new TriggerParams(
                "TEMPORAL",
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 6, 30),
                EnumSet.of(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
                LocalTime.of(19, 0),
                LocalTime.of(23, 0),
                null, null
        );

        var command = new CrearPromocionCommand(
                "Combo Burger",
                "Hamburguesa + gaseosa con 50% off",
                10,
                "COMBO_CONDICIONAL",
                null, null,
                new ComboCondicionalParams(1, new BigDecimal("50")),
                List.of(triggerTemporal)
        );

        when(promocionRepository.existePorNombreYLocal("Combo Burger", localId))
                .thenReturn(false);
        when(promocionRepository.guardar(any(Promocion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PromocionResponse response = useCase.ejecutar(localId, command);

        // Then
        assertNotNull(response);
        assertEquals("Combo Burger", response.nombre());
        assertEquals("COMBO_CONDICIONAL", response.estrategia().tipo());
        assertEquals(1, response.estrategia().cantidadMinimaTrigger());
        assertEquals(new BigDecimal("50"), response.estrategia().porcentajeBeneficio());

        // Validar trigger
        assertEquals(1, response.triggers().size());
        assertEquals("TEMPORAL", response.triggers().get(0).tipo());
    }

    @Test
    void deberia_crear_promocion_con_multiples_triggers() {
        // Given - Promoción con trigger temporal + contenido
        var productoId1 = UUID.randomUUID().toString();
        var productoId2 = UUID.randomUUID().toString();

        var triggerTemporal = new TriggerParams(
                "TEMPORAL",
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 28),
                null, null, null,
                null, null
        );

        var triggerContenido = new TriggerParams(
                "CONTENIDO",
                null, null, null, null, null,
                List.of(productoId1, productoId2),
                null
        );

        var command = new CrearPromocionCommand(
                "Promo combo específico",
                "Solo para productos seleccionados",
                1,
                "DESCUENTO_DIRECTO",
                new DescuentoDirectoParams("PORCENTAJE", new BigDecimal("15")),
                null, null,
                List.of(triggerTemporal, triggerContenido)
        );

        when(promocionRepository.existePorNombreYLocal("Promo combo específico", localId))
                .thenReturn(false);
        when(promocionRepository.guardar(any(Promocion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PromocionResponse response = useCase.ejecutar(localId, command);

        // Then
        assertNotNull(response);
        assertEquals(2, response.triggers().size());
        assertEquals("TEMPORAL", response.triggers().get(0).tipo());
        assertEquals("CONTENIDO", response.triggers().get(1).tipo());
        assertEquals(2, response.triggers().get(1).productosRequeridos().size());
    }

    // ============================================
    // Escenarios de error
    // ============================================

    @Test
    void deberia_rechazar_nombre_duplicado_en_mismo_local() {
        // Given
        var triggerTemporal = new TriggerParams(
                "TEMPORAL",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                null, null, null,
                null, null
        );

        var command = new CrearPromocionCommand(
                "2x1 en cervezas", null, 1,
                "CANTIDAD_FIJA",
                null, new CantidadFijaParams(2, 1), null,
                List.of(triggerTemporal)
        );

        when(promocionRepository.existePorNombreYLocal("2x1 en cervezas", localId))
                .thenReturn(true);

        // When / Then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar(localId, command)
        );

        assertTrue(ex.getMessage().contains("Ya existe"));
        assertTrue(ex.getMessage().contains("2x1 en cervezas"));
        verify(promocionRepository, never()).guardar(any());
    }

    @Test
    void deberia_rechazar_tipo_estrategia_invalido() {
        // Given
        var triggerTemporal = new TriggerParams(
                "TEMPORAL",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                null, null, null,
                null, null
        );

        var command = new CrearPromocionCommand(
                "Promo rara", null, 1,
                "ESTRATEGIA_INEXISTENTE",
                null, null, null,
                List.of(triggerTemporal)
        );

        when(promocionRepository.existePorNombreYLocal("Promo rara", localId))
                .thenReturn(false);

        // When / Then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar(localId, command)
        );

        assertTrue(ex.getMessage().contains("no válido"));
    }

    @Test
    void deberia_rechazar_parametros_faltantes_para_descuento_directo() {
        // Given — tipo DESCUENTO_DIRECTO pero sin parámetros
        var triggerTemporal = new TriggerParams(
                "TEMPORAL",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                null, null, null,
                null, null
        );

        var command = new CrearPromocionCommand(
                "Promo incompleta", null, 1,
                "DESCUENTO_DIRECTO",
                null, null, null,
                List.of(triggerTemporal)
        );

        when(promocionRepository.existePorNombreYLocal("Promo incompleta", localId))
                .thenReturn(false);

        // When / Then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar(localId, command)
        );

        assertTrue(ex.getMessage().contains("obligatorios"));
    }

    @Test
    void deberia_rechazar_prioridad_negativa_via_dominio() {
        // Given — Prioridad negativa: la validación corre en el constructor de Promocion
        var triggerTemporal = new TriggerParams(
                "TEMPORAL",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                null, null, null,
                null, null
        );

        var command = new CrearPromocionCommand(
                "Promo con prioridad inválida", null, -5,
                "DESCUENTO_DIRECTO",
                new DescuentoDirectoParams("PORCENTAJE", new BigDecimal("10")),
                null, null,
                List.of(triggerTemporal)
        );

        when(promocionRepository.existePorNombreYLocal("Promo con prioridad inválida", localId))
                .thenReturn(false);

        // When / Then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar(localId, command)
        );

        assertTrue(ex.getMessage().contains("negativa"));
        verify(promocionRepository, never()).guardar(any());
    }

    @Test
    void deberia_rechazar_trigger_temporal_sin_campos_obligatorios() {
        // Given - Trigger TEMPORAL sin fechaDesde
        var triggerInvalido = new TriggerParams(
                "TEMPORAL",
                null, // fechaDesde missing
                LocalDate.now().plusDays(30),
                null, null, null,
                null, null
        );

        var command = new CrearPromocionCommand(
                "Promo con trigger inválido", null, 1,
                "DESCUENTO_DIRECTO",
                new DescuentoDirectoParams("PORCENTAJE", new BigDecimal("10")),
                null, null,
                List.of(triggerInvalido)
        );

        when(promocionRepository.existePorNombreYLocal("Promo con trigger inválido", localId))
                .thenReturn(false);

        // When / Then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar(localId, command)
        );

        assertTrue(ex.getMessage().contains("obligatorias"));
    }

    @Test
    void deberia_rechazar_trigger_contenido_sin_productos() {
        // Given - Trigger CONTENIDO sin productos
        var triggerInvalido = new TriggerParams(
                "CONTENIDO",
                null, null, null, null, null,
                List.of(), // sin productos
                null
        );

        var command = new CrearPromocionCommand(
                "Promo sin productos", null, 1,
                "CANTIDAD_FIJA",
                null, new CantidadFijaParams(2, 1), null,
                List.of(triggerInvalido)
        );

        when(promocionRepository.existePorNombreYLocal("Promo sin productos", localId))
                .thenReturn(false);

        // When / Then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar(localId, command)
        );

        assertTrue(ex.getMessage().contains("producto"));
    }
}
