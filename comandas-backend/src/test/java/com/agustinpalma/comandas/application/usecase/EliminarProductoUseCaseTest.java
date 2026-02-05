package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del caso de uso EliminarProductoUseCase.
 * Utiliza mocks para aislar la lógica del caso de uso.
 * 
 * Escenarios críticos probados (reglas de negocio HU-19):
 * - Eliminación exitosa de producto propio
 * - Rechazo por producto no encontrado (404)
 * - Rechazo por producto de otro local (403 - seguridad multi-tenancy)
 * - Manejo de integridad referencial (delegado a la BD en producción)
 */
@ExtendWith(MockitoExtension.class)
class EliminarProductoUseCaseTest {

    @Mock
    private ProductoRepository productoRepository;

    private EliminarProductoUseCase useCase;

    private LocalId localId;
    private LocalId otroLocalId;
    private ProductoId productoId;

    @BeforeEach
    void setUp() {
        useCase = new EliminarProductoUseCase(productoRepository);
        localId = LocalId.generate();
        otroLocalId = LocalId.generate();
        productoId = ProductoId.generate();
    }

    @Test
    void deberia_eliminar_producto_con_exito() {
        // Given
        Producto producto = new Producto(
            productoId,
            localId,
            "Producto a Eliminar",
            new BigDecimal("1000.00"),
            true,
            "#FF0000"
        );

        when(productoRepository.buscarPorId(productoId))
            .thenReturn(Optional.of(producto));

        // When
        assertDoesNotThrow(() -> useCase.ejecutar(productoId, localId));

        // Then
        verify(productoRepository).buscarPorId(productoId);
        verify(productoRepository).eliminar(productoId);
    }

    @Test
    void deberia_rechazar_eliminar_producto_no_encontrado() {
        // Given
        when(productoRepository.buscarPorId(productoId))
            .thenReturn(Optional.empty());

        // When / Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> useCase.ejecutar(productoId, localId)
        );

        assertTrue(exception.getMessage().contains("no encontrado"));
        verify(productoRepository, never()).eliminar(any());
    }

    @Test
    void deberia_rechazar_eliminar_producto_de_otro_local() {
        // Given - Producto pertenece a OTRO local
        Producto productoDeOtroLocal = new Producto(
            productoId,
            otroLocalId, // Diferente al localId del usuario
            "Producto Ajeno",
            new BigDecimal("2000.00"),
            true,
            "#00FF00"
        );

        when(productoRepository.buscarPorId(productoId))
            .thenReturn(Optional.of(productoDeOtroLocal));

        // When / Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> useCase.ejecutar(productoId, localId)
        );

        assertTrue(exception.getMessage().contains("permisos"));
        verify(productoRepository, never()).eliminar(any());
    }

    @Test
    void deberia_verificar_ownership_antes_de_eliminar() {
        // Given
        Producto productoPropio = new Producto(
            productoId,
            localId,
            "Mi Producto",
            new BigDecimal("1500.00"),
            true,
            "#0000FF"
        );

        when(productoRepository.buscarPorId(productoId))
            .thenReturn(Optional.of(productoPropio));

        // When
        useCase.ejecutar(productoId, localId);

        // Then - Debe verificar que existe Y que pertenece al local antes de eliminar
        verify(productoRepository).buscarPorId(productoId);
        verify(productoRepository).eliminar(productoId);
    }

    @Test
    void deberia_rechazar_productoId_null() {
        // When / Then
        assertThrows(
            NullPointerException.class,
            () -> useCase.ejecutar(null, localId)
        );

        verify(productoRepository, never()).eliminar(any());
    }

    @Test
    void deberia_rechazar_localId_null() {
        // When / Then
        assertThrows(
            NullPointerException.class,
            () -> useCase.ejecutar(productoId, null)
        );

        verify(productoRepository, never()).eliminar(any());
    }

    @Test
    void deberia_permitir_eliminar_producto_inactivo() {
        // Given - Un producto que fue desactivado previamente
        Producto productoInactivo = new Producto(
            productoId,
            localId,
            "Producto Inactivo",
            new BigDecimal("800.00"),
            false, // Inactivo
            "#AAAAAA"
        );

        when(productoRepository.buscarPorId(productoId))
            .thenReturn(Optional.of(productoInactivo));

        // When
        assertDoesNotThrow(() -> useCase.ejecutar(productoId, localId));

        // Then - Debe permitir eliminar incluso si está inactivo
        verify(productoRepository).eliminar(productoId);
    }
}
