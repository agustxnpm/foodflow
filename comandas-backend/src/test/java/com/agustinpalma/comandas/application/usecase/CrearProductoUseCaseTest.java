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
            "#FF0000",
            null,
            null,
            null,
            null,
            null,
            null
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
            "#00FF00",
            null,
            null,
            null,
            null,
            null,
            null
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
            "#FFFF00",
            null,
            null,
            null,
            null,
            null,
            null
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
            "#abc123", // Minúsculas
            null,
            null,
            null,
            null,
            null,
            null
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
            null, // Sin color
            null,
            null,
            null,
            null,
            null,
            null
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
            "#00AA00",
            null,
            null,
            null,
            null,
            null,
            null
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
    void deberia_crear_producto_con_control_stock_activado() {
        // Given
        ProductoRequest request = new ProductoRequest(
            "Pan de Hamburguesa",
            new BigDecimal("300.00"),
            true,
            "#FFD700",
            true, // Materia prima → controla stock
            null,
            null,
            null,
            null,
            null
        );

        when(productoRepository.existePorNombreYLocal(any(), any())).thenReturn(false);
        when(productoRepository.guardar(any(Producto.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductoResponse response = useCase.ejecutar(localId, request);

        // Then
        assertNotNull(response);
        assertTrue(response.controlaStock(), "Debe activar control de stock");
        assertEquals(0, response.stockActual(), "Stock inicial debe ser 0");
    }

    @Test
    void deberia_crear_producto_sin_control_stock_por_defecto() {
        // Given
        ProductoRequest request = new ProductoRequest(
            "Hamburguesa Completa",
            new BigDecimal("1500.00"),
            true,
            "#FF0000",
            null, // No especificado → default false
            null,
            null,
            null,
            null,
            null
        );

        when(productoRepository.existePorNombreYLocal(any(), any())).thenReturn(false);
        when(productoRepository.guardar(any(Producto.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductoResponse response = useCase.ejecutar(localId, request);

        // Then
        assertFalse(response.controlaStock(), "Menú no controla stock por defecto");
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
            "#FFFFFF",
            null,
            null,
            null,
            null,
            null,
            null
        );

        // When / Then
        assertThrows(
            NullPointerException.class,
            () -> useCase.ejecutar(null, request)
        );
    }

    @Test
    void deberia_crear_producto_como_extra_cuando_esExtra_es_true() {
        // Given — el operador crea "huevo" como extra
        ProductoRequest request = new ProductoRequest(
            "Huevo",
            new BigDecimal("200.00"),
            true,
            "#FFFF00",
            false,
            true,  // esExtra = true
            null,
            null,  // categoria
            null,  // permiteExtras
            null   // requiereConfiguracion
        );

        when(productoRepository.existePorNombreYLocal(any(), any())).thenReturn(false);
        when(productoRepository.guardar(any(Producto.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductoResponse response = useCase.ejecutar(localId, request);

        // Then
        assertNotNull(response);
        assertEquals("Huevo", response.nombre());
        assertTrue(response.esExtra(), "Debe ser marcado como extra");
    }

    @Test
    void deberia_crear_producto_sin_ser_extra_por_defecto() {
        // Given — el operador crea un producto normal sin especificar esExtra
        ProductoRequest request = new ProductoRequest(
            "Coca Cola",
            new BigDecimal("500.00"),
            true,
            "#0000FF",
            null,
            null,  // esExtra = null → default false
            null,
            null,  // categoria
            null,  // permiteExtras
            null   // requiereConfiguracion
        );

        when(productoRepository.existePorNombreYLocal(any(), any())).thenReturn(false);
        when(productoRepository.guardar(any(Producto.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductoResponse response = useCase.ejecutar(localId, request);

        // Then
        assertFalse(response.esExtra(), "Producto normal no es extra por defecto");
    }

    // ─────────────────────────────────────────────────────────────────
    // Tests de categoria y permiteExtras en ProductoResponse
    // ─────────────────────────────────────────────────────────────────

    @Test
    void deberia_crear_producto_con_categoria_y_permiteExtras_en_response() {
        // Given — el operador crea un producto especificando categoría y permiteExtras=false
        ProductoRequest request = new ProductoRequest(
            "Coca Cola 500ml",
            new BigDecimal("800.00"),
            true,
            "#CC0000",
            null,
            null,    // esExtra
            null,
            null,    // categoriaId — ahora es UUID, se testea por separado con categoría real
            false,   // permiteExtras = false (las bebidas no llevan extras)
            null     // requiereConfiguracion
        );

        when(productoRepository.existePorNombreYLocal(any(), any())).thenReturn(false);
        when(productoRepository.guardar(any(Producto.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductoResponse response = useCase.ejecutar(localId, request);

        // Then — el response debe exponer categoriaId y permiteExtras
        assertNull(response.categoriaId(), "CategoriaId debe ser null cuando no se especifica");
        assertFalse(response.permiteExtras(), "Bebida no debe permitir extras");
    }

    @Test
    void deberia_usar_permiteExtras_true_por_defecto_cuando_no_se_envia() {
        // Given — el operador no especifica permiteExtras ni categoria
        ProductoRequest request = new ProductoRequest(
            "Hamburguesa Doble",
            new BigDecimal("2500.00"),
            true,
            "#FF5500",
            null,
            null,  // esExtra
            null,
            null,  // categoriaId = null
            null,  // permiteExtras = null → default true
            null   // requiereConfiguracion
        );

        when(productoRepository.existePorNombreYLocal(any(), any())).thenReturn(false);
        when(productoRepository.guardar(any(Producto.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductoResponse response = useCase.ejecutar(localId, request);

        // Then — por defecto permiteExtras debe ser true y categoriaId null
        assertTrue(response.permiteExtras(), "Producto debe permitir extras por defecto");
        assertNull(response.categoriaId(), "CategoriaId debe ser null si no se especifica");
    }
}
