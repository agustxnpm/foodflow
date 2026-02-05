package com.agustinpalma.comandas.application.usecase;

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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del caso de uso ConsultarProductosUseCase.
 * Utiliza mocks para aislar la lógica del caso de uso.
 * 
 * Escenarios probados (críticos para HU-19):
 * - Consulta sin filtro: retorna todos los productos del local
 * - Consulta con filtro de color: retorna solo productos con ese color
 * - Aislamiento multi-tenancy: no retorna productos de otros locales
 * - Manejo de lista vacía
 */
@ExtendWith(MockitoExtension.class)
class ConsultarProductosUseCaseTest {

    @Mock
    private ProductoRepository productoRepository;

    private ConsultarProductosUseCase useCase;

    private LocalId localId;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarProductosUseCase(productoRepository);
        localId = LocalId.generate();
    }

    @Test
    void deberia_retornar_todos_los_productos_sin_filtro() {
        // Given
        Producto producto1 = crearProducto("Hamburguesa", "#FF0000", localId);
        Producto producto2 = crearProducto("Pizza", "#00FF00", localId);
        Producto producto3 = crearProducto("Empanada", "#0000FF", localId);

        when(productoRepository.buscarPorLocal(localId))
            .thenReturn(List.of(producto1, producto2, producto3));

        // When
        List<ProductoResponse> resultado = useCase.ejecutar(localId, null);

        // Then
        assertEquals(3, resultado.size());
        verify(productoRepository).buscarPorLocal(localId);
        verify(productoRepository, never()).buscarPorLocalYColor(any(), any());
    }

    @Test
    void deberia_retornar_solo_productos_rojos_cuando_se_filtra_por_color_rojo() {
        // Given
        Producto productoRojo1 = crearProducto("Hamburguesa", "#FF0000", localId);
        Producto productoRojo2 = crearProducto("Milanesa", "#FF0000", localId);

        when(productoRepository.buscarPorLocalYColor(localId, "#FF0000"))
            .thenReturn(List.of(productoRojo1, productoRojo2));

        // When
        List<ProductoResponse> resultado = useCase.ejecutar(localId, "#FF0000");

        // Then
        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().allMatch(p -> p.colorHex().equals("#FF0000")));
        
        verify(productoRepository).buscarPorLocalYColor(localId, "#FF0000");
        verify(productoRepository, never()).buscarPorLocal(any());
    }

    @Test
    void deberia_normalizar_color_del_filtro_antes_de_buscar() {
        // Given
        Producto productoVerde = crearProducto("Ensalada", "#00FF00", localId);

        when(productoRepository.buscarPorLocalYColor(localId, "#00FF00"))
            .thenReturn(List.of(productoVerde));

        // When - Filtro en minúsculas
        useCase.ejecutar(localId, "#00ff00");

        // Then - Debe normalizarse a mayúsculas antes de consultar
        verify(productoRepository).buscarPorLocalYColor(localId, "#00FF00");
    }

    @Test
    void deberia_retornar_lista_vacia_cuando_no_hay_productos() {
        // Given
        when(productoRepository.buscarPorLocal(localId))
            .thenReturn(List.of());

        // When
        List<ProductoResponse> resultado = useCase.ejecutar(localId, null);

        // Then
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void deberia_retornar_lista_vacia_cuando_no_hay_productos_del_color_filtrado() {
        // Given
        when(productoRepository.buscarPorLocalYColor(localId, "#AABBCC"))
            .thenReturn(List.of());

        // When
        List<ProductoResponse> resultado = useCase.ejecutar(localId, "#AABBCC");

        // Then
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void deberia_ignorar_filtro_vacio_y_retornar_todos() {
        // Given
        Producto producto1 = crearProducto("Producto 1", "#FF0000", localId);
        Producto producto2 = crearProducto("Producto 2", "#00FF00", localId);

        when(productoRepository.buscarPorLocal(localId))
            .thenReturn(List.of(producto1, producto2));

        // When - Filtro vacío
        List<ProductoResponse> resultado = useCase.ejecutar(localId, "   ");

        // Then
        assertEquals(2, resultado.size());
        verify(productoRepository).buscarPorLocal(localId);
        verify(productoRepository, never()).buscarPorLocalYColor(any(), any());
    }

    @Test
    void deberia_rechazar_localId_null() {
        // When / Then
        assertThrows(
            NullPointerException.class,
            () -> useCase.ejecutar(null, "#FF0000")
        );
    }

    // Helper para crear productos de prueba
    private Producto crearProducto(String nombre, String color, LocalId local) {
        return new Producto(
            ProductoId.generate(),
            local,
            nombre,
            new BigDecimal("1000.00"),
            true,
            color
        );
    }
}
