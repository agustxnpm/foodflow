package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.ProductoRequest;
import com.agustinpalma.comandas.application.dto.ProductoResponse;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del caso de uso CrearProductoUseCase.
 * Utiliza mocks para aislar la lógica del caso de uso.
 * 
 * Escenarios probados:
 * - Creación exitosa con datos válidos
 * - Rechazo por nombre duplicado (409 Conflict)
 * - Normalización automática del color
 * - Asignación de color default cuando es null
 */
@ExtendWith(MockitoExtension.class)
class CrearProductoUseCaseTest {

    @Mock
    private ProductoRepository productoRepository;

    private CrearProductoUseCase useCase;

    private LocalId localId;

    @BeforeEach
    void setUp() {
        useCase = new CrearProductoUseCase(productoRepository);
        localId = LocalId.generate();
    }

    @Test
    void deberia_crear_producto_con_exito() {
        // Given
        ProductoRequest request = new ProductoRequest(
            "Hamburguesa Completa",
            new BigDecimal("1500.00"),
            true,
            "#FF0000"
        );

        when(productoRepository.existePorNombreYLocal("Hamburguesa Completa", localId))
            .thenReturn(false);
        
        when(productoRepository.guardar(any(Producto.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductoResponse response = useCase.ejecutar(localId, request);

        // Then
        assertNotNull(response);
        assertEquals("Hamburguesa Completa", response.nombre());
        assertEquals(new BigDecimal("1500.00"), response.precio());
        assertTrue(response.activo());
        assertEquals("#FF0000", response.colorHex());
        
        verify(productoRepository).existePorNombreYLocal("Hamburguesa Completa", localId);
        verify(productoRepository).guardar(any(Producto.class));
    }

    @Test
    void deberia_rechazar_nombre_duplicado_en_mismo_local() {
        // Given
        ProductoRequest request = new ProductoRequest(
            "Pizza Napolitana",
            new BigDecimal("2000.00"),
            true,
            "#00FF00"
        );

        when(productoRepository.existePorNombreYLocal("Pizza Napolitana", localId))
            .thenReturn(true);

        // When / Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> useCase.ejecutar(localId, request)
        );

        assertTrue(exception.getMessage().contains("Ya existe"));
        assertTrue(exception.getMessage().contains("Pizza Napolitana"));
        
        verify(productoRepository).existePorNombreYLocal("Pizza Napolitana", localId);
        verify(productoRepository, never()).guardar(any());
    }

    @Test
    void deberia_permitir_mismo_nombre_en_diferentes_locales() {
        // Given
        ProductoRequest request = new ProductoRequest(
            "Empanada de Carne",
            new BigDecimal("500.00"),
            true,
            "#FFFF00"
        );

        // El nombre existe en OTRO local, pero no en el actual
        when(productoRepository.existePorNombreYLocal("Empanada de Carne", localId))
            .thenReturn(false);
        
        when(productoRepository.guardar(any(Producto.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductoResponse response = useCase.ejecutar(localId, request);

        // Then
        assertNotNull(response);
        assertEquals("Empanada de Carne", response.nombre());
        verify(productoRepository).guardar(any(Producto.class));
    }

    @Test
    void deberia_normalizar_color_a_mayusculas() {
        // Given
        ProductoRequest request = new ProductoRequest(
            "Milanesa",
            new BigDecimal("1800.00"),
            true,
            "#abc123" // Minúsculas
        );

        when(productoRepository.existePorNombreYLocal(any(), any())).thenReturn(false);
        when(productoRepository.guardar(any(Producto.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductoResponse response = useCase.ejecutar(localId, request);

        // Then
        assertEquals("#ABC123", response.colorHex(), "El color debe normalizarse a mayúsculas");
    }

    @Test
    void deberia_asignar_color_default_cuando_es_null() {
        // Given
        ProductoRequest request = new ProductoRequest(
            "Papas Fritas",
            new BigDecimal("900.00"),
            true,
            null // Sin color
        );

        when(productoRepository.existePorNombreYLocal(any(), any())).thenReturn(false);
        when(productoRepository.guardar(any(Producto.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductoResponse response = useCase.ejecutar(localId, request);

        // Then
        assertEquals("#FFFFFF", response.colorHex(), "Debe asignar color blanco por defecto");
    }

    @Test
    void deberia_crear_producto_activo_por_defecto_cuando_activo_es_null() {
        // Given
        ProductoRequest request = new ProductoRequest(
            "Ensalada César",
            new BigDecimal("1200.00"),
            null, // Estado no especificado
            "#00AA00"
        );

        when(productoRepository.existePorNombreYLocal(any(), any())).thenReturn(false);
        when(productoRepository.guardar(any(Producto.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductoResponse response = useCase.ejecutar(localId, request);

        // Then
        assertTrue(response.activo(), "Debe ser activo por defecto");
    }

    @Test
    void deberia_rechazar_request_null() {
        // When / Then
        assertThrows(
            NullPointerException.class,
            () -> useCase.ejecutar(localId, null)
        );
    }

    @Test
    void deberia_rechazar_localId_null() {
        // Given
        ProductoRequest request = new ProductoRequest(
            "Producto",
            new BigDecimal("1000.00"),
            true,
            "#FFFFFF"
        );

        // When / Then
        assertThrows(
            NullPointerException.class,
            () -> useCase.ejecutar(null, request)
        );
    }
}
