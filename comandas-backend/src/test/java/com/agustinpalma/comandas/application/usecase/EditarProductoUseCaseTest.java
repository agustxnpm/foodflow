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
            "#FF5500",
            null,
            null,
            null,
            null,
            null
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
            "#FFFFFF",
            null,
            null,
            null,
            null,
            null
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
            "#000000",
            null,
            null,
            null,
            null,
            null
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
            "#00FF00",
            null,
            null,
            null,
            null,
            null
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
            "#FF0000",
            null,
            null,
            null,
            null,
            null
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
            "#FF0000",
            null,
            null,
            null,
            null,
            null
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
            "#FFFFFF",
            null,
            null,
            null,
            null,
            null
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
    void deberia_activar_control_stock_al_editar() {
        // Given - Producto sin control de stock (menú)
        Producto productoExistente = new Producto(
            productoId,
            localId,
            "Pan de Hamburguesa",
            new BigDecimal("300.00"),
            true,
            "#FFD700"
        );

        ProductoRequest request = new ProductoRequest(
            "Pan de Hamburguesa",
            new BigDecimal("300.00"),
            true,
            "#FFD700",
            true, // Activar control de stock
            null,
            null,
            null,
            null
        );

        when(productoRepository.buscarPorId(productoId))
            .thenReturn(Optional.of(productoExistente));
        when(productoRepository.guardar(any(Producto.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductoResponse response = useCase.ejecutar(productoId, localId, request);

        // Then
        assertTrue(response.controlaStock(), "Debe activar control de stock");
    }

    @Test
    void deberia_preservar_control_stock_cuando_no_se_envia() {
        // Given - Producto YA controla stock
        Producto productoExistente = new Producto(
            productoId, localId, "Queso Cheddar",
            new BigDecimal("200.00"), true, "#FFD700",
            null, false, null, null, true, true, 50, true // categoria=null, permiteExtras=true, requiereConfiguracion=true, stock=50, controlaStock=true
        );

        ProductoRequest request = new ProductoRequest(
            "Queso Cheddar",
            new BigDecimal("250.00"), // Solo cambia precio
            true,
            "#FFD700",
            null, // No envía controlaStock → se preserva
            null,
            null,
            null,
            null
        );

        when(productoRepository.buscarPorId(productoId))
            .thenReturn(Optional.of(productoExistente));
        when(productoRepository.guardar(any(Producto.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductoResponse response = useCase.ejecutar(productoId, localId, request);

        // Then
        assertTrue(response.controlaStock(), "Debe preservar control de stock activo");
        assertEquals(50, response.stockActual(), "Stock no debe cambiar");
    }

    @Test
    void deberia_rechazar_parametros_null() {
        ProductoRequest request = new ProductoRequest("Test", new BigDecimal("1000"), true, "#FFFFFF", null, null, null, null, null);

        assertThrows(NullPointerException.class, () -> useCase.ejecutar(null, localId, request));
        assertThrows(NullPointerException.class, () -> useCase.ejecutar(productoId, null, request));
        assertThrows(NullPointerException.class, () -> useCase.ejecutar(productoId, localId, null));
    }

    @Test
    void deberia_reclasificar_producto_como_extra() {
        // Given — "huevo" fue creado como producto normal por error (bug pre-fix)
        Producto productoExistente = new Producto(
            productoId, localId, "Huevo",
            new BigDecimal("200.00"), true, "#FFFF00",
            null, false, null, null, true, true, 0, false  // categoria=null, permiteExtras=true, requiereConfiguracion=true, esExtra=false (dato incorrecto)
        );

        ProductoRequest request = new ProductoRequest(
            "Huevo",
            new BigDecimal("200.00"),
            true,
            "#FFFF00",
            false,
            true,  // Corregir: reclasificar como extra
            null,  // categoria
            null,  // permiteExtras
            null   // requiereConfiguracion
        );

        when(productoRepository.buscarPorId(productoId))
            .thenReturn(Optional.of(productoExistente));
        when(productoRepository.guardar(any(Producto.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductoResponse response = useCase.ejecutar(productoId, localId, request);

        // Then
        assertTrue(response.esExtra(), "Debe reclasificarse como extra");
    }

    @Test
    void deberia_preservar_esExtra_cuando_no_se_envia() {
        // Given — producto ya es extra, la edición no envía esExtra
        Producto productoExistente = new Producto(
            productoId, localId, "Queso Cheddar",
            new BigDecimal("200.00"), true, "#FFFF00",
            null, true, null, null, true, true, 0, false  // categoria=null, permiteExtras=true, requiereConfiguracion=true, esExtra=true
        );

        ProductoRequest request = new ProductoRequest(
            "Queso Cheddar",
            new BigDecimal("250.00"), // Solo cambia precio
            true,
            "#FFFF00",
            null,
            null,  // No envía esExtra → se preserva el valor actual
            null,  // categoria
            null,  // permiteExtras
            null   // requiereConfiguracion
        );

        when(productoRepository.buscarPorId(productoId))
            .thenReturn(Optional.of(productoExistente));
        when(productoRepository.guardar(any(Producto.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductoResponse response = useCase.ejecutar(productoId, localId, request);

        // Then
        assertTrue(response.esExtra(), "Debe preservar esExtra = true cuando no se envía");
    }
}
