package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.AsociarScopeCommand;
import com.agustinpalma.comandas.application.dto.AsociarScopeCommand.ItemScopeParams;
import com.agustinpalma.comandas.domain.model.AlcancePromocion;
import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.EstrategiaPromocion;
import com.agustinpalma.comandas.domain.model.Promocion;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import com.agustinpalma.comandas.domain.repository.PromocionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del caso de uso AsociarProductoAPromocionUseCase (HU-09).
 * 
 * Escenarios probados:
 * - Asociación exitosa de productos como TRIGGER
 * - Asociación exitosa de productos como TARGET
 * - Asociación exitosa con mix de TRIGGER y TARGET
 * - Asociación exitosa con categorías
 * - Rechazo cuando promoción no existe
 * - Rechazo cuando producto no existe
 * - Rechazo cuando producto no pertenece al local
 * - Rechazo cuando hay referencias duplicadas
 * - Actualización correcta sobrescribiendo alcance anterior
 */
@ExtendWith(MockitoExtension.class)
class AsociarProductoAPromocionUseCaseTest {

    @Mock
    private PromocionRepository promocionRepository;

    @Mock
    private ProductoRepository productoRepository;

    private AsociarProductoAPromocionUseCase useCase;

    private LocalId localId;
    private PromocionId promocionId;
    private ProductoId productoId1;
    private ProductoId productoId2;
    private ProductoId productoId3;

    @BeforeEach
    void setUp() {
        useCase = new AsociarProductoAPromocionUseCase(promocionRepository, productoRepository);
        localId = LocalId.generate();
        promocionId = PromocionId.generate();
        productoId1 = ProductoId.generate();
        productoId2 = ProductoId.generate();
        productoId3 = ProductoId.generate();
    }

    // ============================================
    // Escenarios exitosos
    // ============================================

    @Test
    void deberia_asociar_productos_como_triggers_exitosamente() {
        // Given
        var promocionExistente = crearPromocionMock();
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.of(promocionExistente));
        when(promocionRepository.guardar(promocionExistente))
                .thenReturn(promocionExistente);
        
        when(productoRepository.buscarPorIdYLocal(productoId1, localId))
                .thenReturn(Optional.of(mock()));
        when(productoRepository.buscarPorIdYLocal(productoId2, localId))
                .thenReturn(Optional.of(mock()));

        var items = List.of(
                new ItemScopeParams(productoId1.getValue().toString(), TipoAlcance.PRODUCTO, RolPromocion.TRIGGER),
                new ItemScopeParams(productoId2.getValue().toString(), TipoAlcance.PRODUCTO, RolPromocion.TRIGGER)
        );
        var command = new AsociarScopeCommand(items);

        // When
        useCase.ejecutar(localId, promocionId, command);

        // Then
        ArgumentCaptor<AlcancePromocion> alcanceCaptor = ArgumentCaptor.forClass(AlcancePromocion.class);
        verify(promocionExistente).definirAlcance(alcanceCaptor.capture());
        verify(promocionRepository).guardar(promocionExistente);

        AlcancePromocion alcanceGuardado = alcanceCaptor.getValue();
        assertEquals(2, alcanceGuardado.getItems().size());
        assertTrue(alcanceGuardado.getItems().stream()
                .allMatch(item -> item.getRol() == RolPromocion.TRIGGER));
    }

    @Test
    void deberia_asociar_productos_como_targets_exitosamente() {
        // Given
        var promocionExistente = crearPromocionMock();
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.of(promocionExistente));
        when(promocionRepository.guardar(promocionExistente))
                .thenReturn(promocionExistente);
        
        when(productoRepository.buscarPorIdYLocal(productoId1, localId))
                .thenReturn(Optional.of(mock()));

        var items = List.of(
                new ItemScopeParams(productoId1.getValue().toString(), TipoAlcance.PRODUCTO, RolPromocion.TARGET)
        );
        var command = new AsociarScopeCommand(items);

        // When
        useCase.ejecutar(localId, promocionId, command);

        // Then
        ArgumentCaptor<AlcancePromocion> alcanceCaptor = ArgumentCaptor.forClass(AlcancePromocion.class);
        verify(promocionExistente).definirAlcance(alcanceCaptor.capture());

        AlcancePromocion alcanceGuardado = alcanceCaptor.getValue();
        assertEquals(1, alcanceGuardado.getItems().size());
        assertEquals(RolPromocion.TARGET, alcanceGuardado.getItems().get(0).getRol());
    }

    @Test
    void deberia_asociar_mix_de_triggers_y_targets() {
        // Given
        var promocionExistente = crearPromocionMock();
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.of(promocionExistente));
        when(promocionRepository.guardar(promocionExistente))
                .thenReturn(promocionExistente);
        
        when(productoRepository.buscarPorIdYLocal(productoId1, localId))
                .thenReturn(Optional.of(mock()));
        when(productoRepository.buscarPorIdYLocal(productoId2, localId))
                .thenReturn(Optional.of(mock()));
        when(productoRepository.buscarPorIdYLocal(productoId3, localId))
                .thenReturn(Optional.of(mock()));

        var items = List.of(
                new ItemScopeParams(productoId1.getValue().toString(), TipoAlcance.PRODUCTO, RolPromocion.TRIGGER),
                new ItemScopeParams(productoId2.getValue().toString(), TipoAlcance.PRODUCTO, RolPromocion.TARGET),
                new ItemScopeParams(productoId3.getValue().toString(), TipoAlcance.PRODUCTO, RolPromocion.TARGET)
        );
        var command = new AsociarScopeCommand(items);

        // When
        useCase.ejecutar(localId, promocionId, command);

        // Then
        ArgumentCaptor<AlcancePromocion> alcanceCaptor = ArgumentCaptor.forClass(AlcancePromocion.class);
        verify(promocionExistente).definirAlcance(alcanceCaptor.capture());

        AlcancePromocion alcanceGuardado = alcanceCaptor.getValue();
        assertEquals(3, alcanceGuardado.getItems().size());
        
        var triggers = alcanceGuardado.getTriggers();
        var targets = alcanceGuardado.getTargets();
        
        assertEquals(1, triggers.size());
        assertEquals(2, targets.size());
    }

    @Test
    void deberia_asociar_categorias_como_targets() {
        // Given
        var promocionExistente = crearPromocionMock();
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.of(promocionExistente));
        when(promocionRepository.guardar(promocionExistente))
                .thenReturn(promocionExistente);

        UUID categoriaId = UUID.randomUUID();
        
        var items = List.of(
                new ItemScopeParams(categoriaId.toString(), TipoAlcance.CATEGORIA, RolPromocion.TARGET)
        );
        var command = new AsociarScopeCommand(items);

        // When
        useCase.ejecutar(localId, promocionId, command);

        // Then
        ArgumentCaptor<AlcancePromocion> alcanceCaptor = ArgumentCaptor.forClass(AlcancePromocion.class);
        verify(promocionExistente).definirAlcance(alcanceCaptor.capture());

        AlcancePromocion alcanceGuardado = alcanceCaptor.getValue();
        assertEquals(1, alcanceGuardado.getItems().size());
        assertEquals(TipoAlcance.CATEGORIA, alcanceGuardado.getItems().get(0).getTipo());
        
        // Las categorías no requieren validación de existencia
        verify(productoRepository, never()).buscarPorIdYLocal(any(), any());
    }

    @Test
    void deberia_sobrescribir_alcance_anterior_al_actualizar() {
        // Given
        var promocionExistente = crearPromocionMock();
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.of(promocionExistente));
        when(promocionRepository.guardar(promocionExistente))
                .thenReturn(promocionExistente);
        
        when(productoRepository.buscarPorIdYLocal(productoId1, localId))
                .thenReturn(Optional.of(mock()));

        var items = List.of(
                new ItemScopeParams(productoId1.getValue().toString(), TipoAlcance.PRODUCTO, RolPromocion.TRIGGER)
        );
        var command = new AsociarScopeCommand(items);

        // When
        useCase.ejecutar(localId, promocionId, command);

        // Then
        // definirAlcance() sobrescribe el alcance anterior
        verify(promocionExistente, times(1)).definirAlcance(any());
        verify(promocionRepository).guardar(promocionExistente);
    }

    @Test
    void deberia_permitir_alcance_vacio() {
        // Given
        var promocionExistente = crearPromocionMock();
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.of(promocionExistente));
        when(promocionRepository.guardar(promocionExistente))
                .thenReturn(promocionExistente);

        var command = new AsociarScopeCommand(List.of());

        // When
        useCase.ejecutar(localId, promocionId, command);

        // Then
        ArgumentCaptor<AlcancePromocion> alcanceCaptor = ArgumentCaptor.forClass(AlcancePromocion.class);
        verify(promocionExistente).definirAlcance(alcanceCaptor.capture());

        AlcancePromocion alcanceGuardado = alcanceCaptor.getValue();
        assertFalse(alcanceGuardado.tieneItems());
    }

    // ============================================
    // Escenarios de rechazo
    // ============================================

    @Test
    void deberia_rechazar_si_promocion_no_existe() {
        // Given
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.empty());

        var items = List.of(
                new ItemScopeParams(productoId1.getValue().toString(), TipoAlcance.PRODUCTO, RolPromocion.TRIGGER)
        );
        var command = new AsociarScopeCommand(items);

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> 
                useCase.ejecutar(localId, promocionId, command));

        verify(promocionRepository, never()).guardar(any());
    }

    @Test
    void deberia_rechazar_si_producto_no_existe() {
        // Given
        var promocionExistente = crearPromocionMock();
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.of(promocionExistente));
        
        when(productoRepository.buscarPorIdYLocal(productoId1, localId))
                .thenReturn(Optional.empty());

        var items = List.of(
                new ItemScopeParams(productoId1.getValue().toString(), TipoAlcance.PRODUCTO, RolPromocion.TRIGGER)
        );
        var command = new AsociarScopeCommand(items);

        // When / Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
                useCase.ejecutar(localId, promocionId, command));

        assertTrue(exception.getMessage().contains("no existe") || 
                   exception.getMessage().contains("No existe"));
        verify(promocionRepository, never()).guardar(any());
    }

    @Test
    void deberia_rechazar_si_producto_no_pertenece_al_local() {
        // Given
        var promocionExistente = crearPromocionMock();
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.of(promocionExistente));
        
        // Producto existe pero en otro local
        when(productoRepository.buscarPorIdYLocal(productoId1, localId))
                .thenReturn(Optional.empty());

        var items = List.of(
                new ItemScopeParams(productoId1.getValue().toString(), TipoAlcance.PRODUCTO, RolPromocion.TARGET)
        );
        var command = new AsociarScopeCommand(items);

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> 
                useCase.ejecutar(localId, promocionId, command));

        verify(promocionRepository, never()).guardar(any());
    }

    @Test
    void deberia_rechazar_referencias_duplicadas() {
        // Given
        var promocionExistente = crearPromocionMock();
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.of(promocionExistente));
        
        when(productoRepository.buscarPorIdYLocal(productoId1, localId))
                .thenReturn(Optional.of(mock()));

        // Mismo producto dos veces
        var items = List.of(
                new ItemScopeParams(productoId1.getValue().toString(), TipoAlcance.PRODUCTO, RolPromocion.TRIGGER),
                new ItemScopeParams(productoId1.getValue().toString(), TipoAlcance.PRODUCTO, RolPromocion.TARGET)
        );
        var command = new AsociarScopeCommand(items);

        // When / Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
                useCase.ejecutar(localId, promocionId, command));

        assertTrue(exception.getMessage().contains("duplicad") || 
                   exception.getMessage().contains("repetid"));
        verify(promocionRepository, never()).guardar(any());
    }

    @Test
    void deberia_validar_todos_los_productos_antes_de_guardar() {
        // Given
        var promocionExistente = crearPromocionMock();
        when(promocionRepository.buscarPorIdYLocal(promocionId, localId))
                .thenReturn(Optional.of(promocionExistente));
        
        when(productoRepository.buscarPorIdYLocal(productoId1, localId))
                .thenReturn(Optional.of(mock()));
        when(productoRepository.buscarPorIdYLocal(productoId2, localId))
                .thenReturn(Optional.empty()); // Este no existe

        var items = List.of(
                new ItemScopeParams(productoId1.getValue().toString(), TipoAlcance.PRODUCTO, RolPromocion.TRIGGER),
                new ItemScopeParams(productoId2.getValue().toString(), TipoAlcance.PRODUCTO, RolPromocion.TARGET)
        );
        var command = new AsociarScopeCommand(items);

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> 
                useCase.ejecutar(localId, promocionId, command));

        // No debe guardar si algún producto es inválido
        verify(promocionRepository, never()).guardar(any());
    }

    // ============================================
    // Helpers
    // ============================================

    private Promocion crearPromocionMock() {
        Promocion promo = mock(Promocion.class);
        lenient().when(promo.getId()).thenReturn(promocionId);
        lenient().when(promo.getLocalId()).thenReturn(localId);
        lenient().when(promo.getNombre()).thenReturn("Promo Test");
        lenient().when(promo.getDescripcion()).thenReturn("Descripción Test");
        lenient().when(promo.getPrioridad()).thenReturn(1);
        lenient().when(promo.getEstado()).thenReturn(EstadoPromocion.ACTIVA);
        lenient().when(promo.getEstrategia()).thenReturn(
                new EstrategiaPromocion.DescuentoDirecto(ModoDescuento.PORCENTAJE, new BigDecimal("10"))
        );
        lenient().when(promo.getTriggers()).thenReturn(Collections.emptyList());
        lenient().when(promo.getAlcance()).thenReturn(new AlcancePromocion(Collections.emptyList()));
        return promo;
    }
}
