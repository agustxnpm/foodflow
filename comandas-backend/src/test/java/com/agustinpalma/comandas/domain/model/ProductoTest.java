package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios puros de la entidad Producto del dominio.
 * No utiliza Spring ni base de datos - solo lógica de negocio.
 * 
 * Casos probados:
 * - Creación válida de producto
 * - Validaciones de precio (debe ser > 0)
 * - Validaciones de nombre (no vacío)
 * - Validación de formato hexadecimal de color
 * - Normalización de color a mayúsculas
 * - Asignación de color por defecto cuando es null
 * - Actualización de atributos
 */
class ProductoTest {

    private static final ProductoId ID_VALIDO = ProductoId.generate();
    private static final LocalId LOCAL_ID_VALIDO = LocalId.generate();

    @Test
    void deberia_crear_producto_con_datos_validos() {
        // Given / When
        Producto producto = new Producto(
            ID_VALIDO,
            LOCAL_ID_VALIDO,
            "Hamburguesa",
            new BigDecimal("1500.00"),
            true,
            "#FF0000"
        );

        // Then
        assertNotNull(producto);
        assertEquals("Hamburguesa", producto.getNombre());
        assertEquals(new BigDecimal("1500.00"), producto.getPrecio());
        assertTrue(producto.isActivo());
        assertEquals("#FF0000", producto.getColorHex());
    }

    @Test
    void deberia_normalizar_color_a_mayusculas() {
        // Given / When
        Producto producto = new Producto(
            ID_VALIDO,
            LOCAL_ID_VALIDO,
            "Pizza",
            new BigDecimal("2000.00"),
            true,
            "#ff5733" // Minúsculas
        );

        // Then
        assertEquals("#FF5733", producto.getColorHex(), "El color debe normalizarse a mayúsculas");
    }

    @Test
    void deberia_asignar_color_default_cuando_es_null() {
        // Given / When
        Producto producto = new Producto(
            ID_VALIDO,
            LOCAL_ID_VALIDO,
            "Empanada",
            new BigDecimal("500.00"),
            true,
            null // Sin color
        );

        // Then
        assertEquals("#FFFFFF", producto.getColorHex(), "Debe asignar color blanco por defecto");
    }

    @Test
    void deberia_asignar_color_default_cuando_es_vacio() {
        // Given / When
        Producto producto = new Producto(
            ID_VALIDO,
            LOCAL_ID_VALIDO,
            "Tarta",
            new BigDecimal("800.00"),
            true,
            "   " // Solo espacios
        );

        // Then
        assertEquals("#FFFFFF", producto.getColorHex(), "Debe asignar color blanco cuando es vacío");
    }

    @Test
    void deberia_aceptar_color_formato_corto() {
        // Given / When
        Producto producto = new Producto(
            ID_VALIDO,
            LOCAL_ID_VALIDO,
            "Café",
            new BigDecimal("300.00"),
            true,
            "#F00" // Formato corto válido
        );

        // Then
        assertEquals("#F00", producto.getColorHex());
    }

    @Test
    void deberia_rechazar_color_sin_hash() {
        // Given / When / Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Producto(
                ID_VALIDO,
                LOCAL_ID_VALIDO,
                "Milanesa",
                new BigDecimal("1800.00"),
                true,
                "FF0000" // Sin #
            )
        );

        assertTrue(exception.getMessage().contains("formato hexadecimal válido"));
    }

    @Test
    void deberia_rechazar_color_con_caracteres_invalidos() {
        // Given / When / Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Producto(
                ID_VALIDO,
                LOCAL_ID_VALIDO,
                "Papas Fritas",
                new BigDecimal("900.00"),
                true,
                "#GGHHII" // Caracteres inválidos
            )
        );

        assertTrue(exception.getMessage().contains("formato hexadecimal válido"));
    }

    @Test
    void deberia_rechazar_color_con_longitud_invalida() {
        // Given / When / Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Producto(
                ID_VALIDO,
                LOCAL_ID_VALIDO,
                "Ensalada",
                new BigDecimal("700.00"),
                true,
                "#FF" // Muy corto
            )
        );

        assertTrue(exception.getMessage().contains("formato hexadecimal válido"));
    }

    @Test
    void deberia_rechazar_precio_cero() {
        // Given / When / Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Producto(
                ID_VALIDO,
                LOCAL_ID_VALIDO,
                "Gratis",
                BigDecimal.ZERO,
                true,
                "#00FF00"
            )
        );

        assertTrue(exception.getMessage().contains("mayor a cero"));
    }

    @Test
    void deberia_rechazar_precio_negativo() {
        // Given / When / Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Producto(
                ID_VALIDO,
                LOCAL_ID_VALIDO,
                "Descuento",
                new BigDecimal("-100.00"),
                true,
                "#0000FF"
            )
        );

        assertTrue(exception.getMessage().contains("mayor a cero"));
    }

    @Test
    void deberia_rechazar_precio_null() {
        // Given / When / Then
        assertThrows(
            IllegalArgumentException.class,
            () -> new Producto(
                ID_VALIDO,
                LOCAL_ID_VALIDO,
                "Sin Precio",
                null,
                true,
                "#AABBCC"
            )
        );
    }

    @Test
    void deberia_rechazar_nombre_vacio() {
        // Given / When / Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Producto(
                ID_VALIDO,
                LOCAL_ID_VALIDO,
                "   ",
                new BigDecimal("1000.00"),
                true,
                "#FFFFFF"
            )
        );

        assertTrue(exception.getMessage().contains("vacío"));
    }

    @Test
    void deberia_rechazar_nombre_null() {
        // Given / When / Then
        assertThrows(
            IllegalArgumentException.class,
            () -> new Producto(
                ID_VALIDO,
                LOCAL_ID_VALIDO,
                null,
                new BigDecimal("1000.00"),
                true,
                "#FFFFFF"
            )
        );
    }

    @Test
    void deberia_actualizar_nombre_correctamente() {
        // Given
        Producto producto = new Producto(
            ID_VALIDO,
            LOCAL_ID_VALIDO,
            "Nombre Original",
            new BigDecimal("1000.00"),
            true,
            "#FF0000"
        );

        // When
        producto.actualizarNombre("Nombre Actualizado");

        // Then
        assertEquals("Nombre Actualizado", producto.getNombre());
    }

    @Test
    void deberia_actualizar_precio_correctamente() {
        // Given
        Producto producto = new Producto(
            ID_VALIDO,
            LOCAL_ID_VALIDO,
            "Producto",
            new BigDecimal("1000.00"),
            true,
            "#FF0000"
        );

        // When
        producto.actualizarPrecio(new BigDecimal("1500.00"));

        // Then
        assertEquals(new BigDecimal("1500.00"), producto.getPrecio());
    }

    @Test
    void deberia_rechazar_actualizacion_precio_invalido() {
        // Given
        Producto producto = new Producto(
            ID_VALIDO,
            LOCAL_ID_VALIDO,
            "Producto",
            new BigDecimal("1000.00"),
            true,
            "#FF0000"
        );

        // When / Then
        assertThrows(
            IllegalArgumentException.class,
            () -> producto.actualizarPrecio(BigDecimal.ZERO)
        );
    }

    @Test
    void deberia_cambiar_estado_correctamente() {
        // Given
        Producto producto = new Producto(
            ID_VALIDO,
            LOCAL_ID_VALIDO,
            "Producto",
            new BigDecimal("1000.00"),
            true,
            "#FF0000"
        );

        // When
        producto.cambiarEstado(false);

        // Then
        assertFalse(producto.isActivo());
    }

    @Test
    void deberia_actualizar_color_correctamente() {
        // Given
        Producto producto = new Producto(
            ID_VALIDO,
            LOCAL_ID_VALIDO,
            "Producto",
            new BigDecimal("1000.00"),
            true,
            "#FF0000"
        );

        // When
        producto.actualizarColor("#00FF00");

        // Then
        assertEquals("#00FF00", producto.getColorHex());
    }

    @Test
    void deberia_normalizar_color_al_actualizar() {
        // Given
        Producto producto = new Producto(
            ID_VALIDO,
            LOCAL_ID_VALIDO,
            "Producto",
            new BigDecimal("1000.00"),
            true,
            "#FF0000"
        );

        // When
        producto.actualizarColor("#abc123"); // Minúsculas

        // Then
        assertEquals("#ABC123", producto.getColorHex(), "El color actualizado debe normalizarse");
    }

    @Test
    void deberia_rechazar_actualizacion_color_invalido() {
        // Given
        Producto producto = new Producto(
            ID_VALIDO,
            LOCAL_ID_VALIDO,
            "Producto",
            new BigDecimal("1000.00"),
            true,
            "#FF0000"
        );

        // When / Then
        assertThrows(
            IllegalArgumentException.class,
            () -> producto.actualizarColor("INVALIDO")
        );
    }

    @Test
    void deberia_comparar_productos_por_id() {
        // Given
        ProductoId id = ProductoId.generate();
        Producto producto1 = new Producto(id, LOCAL_ID_VALIDO, "Producto 1", new BigDecimal("1000"), true, "#FF0000");
        Producto producto2 = new Producto(id, LOCAL_ID_VALIDO, "Producto 2", new BigDecimal("2000"), false, "#00FF00");

        // When / Then
        assertEquals(producto1, producto2, "Productos con mismo ID deben ser iguales");
        assertEquals(producto1.hashCode(), producto2.hashCode());
    }

    // ============================================
    // Tests: asignarGrupoVariante
    // ============================================

    @Test
    void deberia_asignar_grupo_variante_a_producto_sin_grupo() {
        // Given: producto sin grupo de variantes
        ProductoId productoId = ProductoId.generate();
        Producto producto = new Producto(
            productoId, LOCAL_ID_VALIDO, "Hamburguesa", new BigDecimal("1500"), true, "#FF0000"
        );
        assertNull(producto.getGrupoVarianteId());
        assertNull(producto.getCantidadDiscosCarne());
        assertFalse(producto.tieneVariantesEstructurales());

        // When: se asigna como líder de su propio grupo
        producto.asignarGrupoVariante(productoId, 1);

        // Then
        assertEquals(productoId, producto.getGrupoVarianteId());
        assertEquals(1, producto.getCantidadDiscosCarne());
        assertTrue(producto.tieneVariantesEstructurales());
    }

    @Test
    void deberia_rechazar_asignacion_si_ya_tiene_grupo() {
        // Given: producto que ya pertenece a un grupo
        ProductoId grupoId = ProductoId.generate();
        Producto producto = new Producto(
            ProductoId.generate(), LOCAL_ID_VALIDO, "Hamburguesa Doble",
            new BigDecimal("2200"), true, "#FF0000",
            grupoId, false, 2
        );

        // When/Then: intentar reasignar lanza excepción
        ProductoId otroGrupo = ProductoId.generate();
        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> producto.asignarGrupoVariante(otroGrupo, 3)
        );
        assertTrue(ex.getMessage().contains("ya pertenece al grupo de variantes"));
    }

    @Test
    void deberia_rechazar_asignacion_con_grupo_null() {
        // Given
        Producto producto = new Producto(
            ProductoId.generate(), LOCAL_ID_VALIDO, "Pizza", new BigDecimal("3000"), true, "#FF0000"
        );

        // When/Then
        assertThrows(
            NullPointerException.class,
            () -> producto.asignarGrupoVariante(null, 1)
        );
    }
}
