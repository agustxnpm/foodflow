package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.ProductoRequest;
import com.agustinpalma.comandas.application.dto.ProductoResponse;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del caso de uso EditarProductoUseCase.
 * Utiliza mocks para aislar la lógica del caso de uso.
 * 
 * Escenarios críticos probados (reglas de negocio HU-19):
 * - Edición exitosa de producto propio
 * - Rechazo por producto no encontrado (404)
 * - Rechazo por producto de otro local (403 - seguridad multi-tenancy)
 * - Rechazo por nombre duplicado al editar (409)
 * - Permitir mismo nombre si no cambió
 */
@ExtendWith(MockitoExtension.class)
class EditarProductoUseCaseTest {

    @Mock
    private ProductoRepository productoRepository;

    private EditarProductoUseCase useCase;

    private LocalId localId;
    private LocalId otroLocalId;
    private ProductoId productoId;

    @BeforeEach
    void setUp() {
        useCase = new EditarProductoUseCase(productoRepository);
        localId = LocalId.generate();
        otroLocalId = LocalId.generate();
        productoId = ProductoId.generate();
    }

    @Test
    void deberia_editar_producto_con_exito() {
        // Given
        Producto productoExistente = new Producto(
            productoId,
            localId,
            "Hamburguesa Simple",
            new BigDecimal("1200.00"),
            true,
            "#FF0000"
        );

        ProductoRequest request = new ProductoRequest(
            "Hamburguesa Completa",
            new BigDecimal("1500.00"),
            true,
            "#FF5500"
        );

        when(productoRepository.buscarPorId(productoId))
            .thenReturn(Optional.of(productoExistente));
        
        when(productoRepository.existePorNombreYLocalExcluyendo(
            "Hamburguesa Completa", localId, productoId))
            .thenReturn(false);
        
        when(productoRepository.guardar(any(Producto.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductoResponse response = useCase.ejecutar(productoId, localId, request);

        // Then
        assertNotNull(response);
        assertEquals("Hamburguesa Completa", response.nombre());
        assertEquals(new BigDecimal("1500.00"), response.precio());
        assertEquals("#FF5500", response.colorHex());
        
        verify(productoRepository).guardar(any(Producto.class));
    }

    @Test
    void deberia_rechazar_editar_producto_no_encontrado() {
        // Given
        ProductoRequest request = new ProductoRequest(
            "Producto Fantasma",
            new BigDecimal("1000.00"),
            true,
            "#FFFFFF"
        );

        when(productoRepository.buscarPorId(productoId))
            .thenReturn(Optional.empty());

        // When / Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> useCase.ejecutar(productoId, localId, request)
        );

        assertTrue(exception.getMessage().contains("no encontrado"));
        verify(productoRepository, never()).guardar(any());
    }

    @Test
    void deberia_rechazar_editar_producto_de_otro_local() {
        // Given - Producto pertenece a OTRO local
        Producto productoDeOtroLocal = new Producto(
            productoId,
            otroLocalId, // Diferente al localId del usuario
            "Pizza Ajena",
            new BigDecimal("2000.00"),
            true,
            "#00FF00"
        );

        ProductoRequest request = new ProductoRequest(
            "Intento de Hackeo",
            new BigDecimal("1.00"),
            false,
            "#000000"
        );

        when(productoRepository.buscarPorId(productoId))
            .thenReturn(Optional.of(productoDeOtroLocal));

        // When / Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> useCase.ejecutar(productoId, localId, request)
        );

        assertTrue(exception.getMessage().contains("permisos"));
        verify(productoRepository, never()).guardar(any());
    }

    @Test
    void deberia_rechazar_nombre_duplicado_al_editar() {
        // Given
        Producto productoExistente = new Producto(
            productoId,
            localId,
            "Empanada de Carne",
            new BigDecimal("500.00"),
            true,
            "#FFFF00"
        );

        ProductoRequest request = new ProductoRequest(
            "Pizza Napolitana", // Este nombre ya existe en otro producto
            new BigDecimal("2000.00"),
            true,
            "#00FF00"
        );

        when(productoRepository.buscarPorId(productoId))
            .thenReturn(Optional.of(productoExistente));
        
        // Otro producto ya tiene este nombre
        when(productoRepository.existePorNombreYLocalExcluyendo(
            "Pizza Napolitana", localId, productoId))
            .thenReturn(true);

        // When / Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> useCase.ejecutar(productoId, localId, request)
        );

        assertTrue(exception.getMessage().contains("Ya existe otro producto"));
        verify(productoRepository, never()).guardar(any());
    }

    @Test
    void deberia_permitir_mantener_mismo_nombre() {
        // Given - El producto mantiene su nombre
        Producto productoExistente = new Producto(
            productoId,
            localId,
            "Milanesa Napolitana",
            new BigDecimal("1800.00"),
            true,
            "#FF0000"
        );

        ProductoRequest request = new ProductoRequest(
            "Milanesa Napolitana", // MISMO nombre (solo cambio precio)
            new BigDecimal("2000.00"),
            true,
            "#FF0000"
        );

        when(productoRepository.buscarPorId(productoId))
            .thenReturn(Optional.of(productoExistente));
        
        when(productoRepository.guardar(any(Producto.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductoResponse response = useCase.ejecutar(productoId, localId, request);

        // Then
        assertNotNull(response);
        assertEquals(new BigDecimal("2000.00"), response.precio());
        
        // NO debe verificar duplicados si el nombre no cambió
        verify(productoRepository, never()).existePorNombreYLocalExcluyendo(any(), any(), any());
        verify(productoRepository).guardar(any(Producto.class));
    }

    @Test
    void deberia_permitir_cambio_solo_de_case_en_nombre() {
        // Given
        Producto productoExistente = new Producto(
            productoId,
            localId,
            "hamburguesa simple",
            new BigDecimal("1200.00"),
            true,
            "#FF0000"
        );

        ProductoRequest request = new ProductoRequest(
            "Hamburguesa Simple", // MISMO nombre, diferente case
            new BigDecimal("1200.00"),
            true,
            "#FF0000"
        );

        when(productoRepository.buscarPorId(productoId))
            .thenReturn(Optional.of(productoExistente));
        
        when(productoRepository.guardar(any(Producto.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductoResponse response = useCase.ejecutar(productoId, localId, request);

        // Then
        assertNotNull(response);
        assertEquals("Hamburguesa Simple", response.nombre());
        
        // NO debe fallar por "duplicado" porque es el mismo producto
        verify(productoRepository, never()).existePorNombreYLocalExcluyendo(any(), any(), any());
    }

    @Test
    void deberia_actualizar_estado_correctamente() {
        // Given
        Producto productoExistente = new Producto(
            productoId,
            localId,
            "Producto Activo",
            new BigDecimal("1000.00"),
            true,
            "#FFFFFF"
        );

        ProductoRequest request = new ProductoRequest(
            "Producto Activo",
            new BigDecimal("1000.00"),
            false, // Desactivar
            "#FFFFFF"
        );

        when(productoRepository.buscarPorId(productoId))
            .thenReturn(Optional.of(productoExistente));
        
        when(productoRepository.guardar(any(Producto.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductoResponse response = useCase.ejecutar(productoId, localId, request);

        // Then
        assertFalse(response.activo());
    }

    @Test
    void deberia_rechazar_parametros_null() {
        ProductoRequest request = new ProductoRequest("Test", new BigDecimal("1000"), true, "#FFFFFF");

        assertThrows(NullPointerException.class, () -> useCase.ejecutar(null, localId, request));
        assertThrows(NullPointerException.class, () -> useCase.ejecutar(productoId, null, request));
        assertThrows(NullPointerException.class, () -> useCase.ejecutar(productoId, localId, null));
    }
}
