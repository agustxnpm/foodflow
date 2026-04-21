package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios puros para el Value Object ExtraPedido.
 *
 * ExtraPedido es un snapshot inmutable: captura nombre y precio del producto
 * extra en el momento de agregarlo al ítem, y NUNCA cambia después
 * (consistencia histórica frente a cambios de catálogo).
 *
 * Escenarios:
 * - Factory crearDesdeProducto sólo acepta productos con esExtra=true.
 * - Snapshot inmutable: cambios posteriores en el Producto NO afectan al ExtraPedido.
 * - Comparación por valor (Value Object): mismo nombre+precio+productoId → equals.
 * - Validación: nombre vacío y precio negativo son rechazados.
 *
 * Sin Spring. Dominio puro.
 */
class ExtraPedidoTest {

    private final LocalId localId = LocalId.generate();

    /** Crea un Producto marcado como extra (esExtra=true). Constructor completo. */
    private Producto productoExtra(String nombre, String precio) {
        return new Producto(
            ProductoId.generate(), localId, nombre,
            new BigDecimal(precio), true, "#FFFFFF",
            null,    // grupoVarianteId
            true,    // esExtra
            false,   // esModificadorEstructural
            null,    // cantidadDiscosCarne
            null,    // categoriaId
            true,    // permiteExtras
            true,    // requiereConfiguracion
            0,       // stockActual
            false    // controlaStock
        );
    }

    @Test
    void deberia_crear_extra_desde_producto_marcado_como_extra() {
        // Given — un producto marcado como extra ($150)
        Producto huevo = productoExtra("Huevo", "150");

        // When — factory de snapshot
        ExtraPedido extra = ExtraPedido.crearDesdeProducto(huevo);

        // Then — captura snapshot del estado actual del producto
        assertThat(extra.getProductoId()).isEqualTo(huevo.getId());
        assertThat(extra.getNombre()).isEqualTo("Huevo");
        assertThat(extra.getPrecioSnapshot()).isEqualByComparingTo("150");
    }

    @Test
    void deberia_rechazar_producto_no_marcado_como_extra() {
        // Given — producto SIN marca esExtra=true (constructor de retrocompatibilidad)
        Producto pizza = new Producto(
            ProductoId.generate(), localId, "Pizza",
            new BigDecimal("1000"), true, "#FF0000"
        );  // esExtra=false por defecto

        // When / Then — la factory protege la invariante
        assertThatThrownBy(() -> ExtraPedido.crearDesdeProducto(pizza))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no está marcado como extra");
    }

    @Test
    void deberia_mantener_snapshot_inmutable_aunque_cambie_el_producto_origen() {
        // Given — extra creado con precio $200
        Producto bacon = productoExtra("Bacon", "200");
        ExtraPedido extra = ExtraPedido.crearDesdeProducto(bacon);

        // When — el catálogo modifica el precio del producto original
        bacon.actualizarPrecio(new BigDecimal("999"));

        // Then — el snapshot conserva el precio histórico ($200, no $999)
        assertThat(extra.getPrecioSnapshot()).isEqualByComparingTo("200");
        assertThat(bacon.getPrecio()).isEqualByComparingTo("999");
    }

    @Test
    void deberia_comparar_extras_por_valor_no_por_identidad() {
        // Given — dos extras con MISMO productoId, nombre y precio (Value Object)
        ProductoId id = ProductoId.generate();
        ExtraPedido extra1 = new ExtraPedido(id, "Queso", new BigDecimal("100"));
        ExtraPedido extra2 = new ExtraPedido(id, "Queso", new BigDecimal("100"));

        // Then — son iguales por valor (no son la misma instancia, pero equivalen)
        assertThat(extra1).isEqualTo(extra2);
        assertThat(extra1.hashCode()).isEqualTo(extra2.hashCode());
        assertThat(extra1).isNotSameAs(extra2);

        // Y un extra con precio distinto NO es igual
        ExtraPedido extraDistinto = new ExtraPedido(id, "Queso", new BigDecimal("200"));
        assertThat(extra1).isNotEqualTo(extraDistinto);
    }

    @Test
    void deberia_rechazar_nombre_vacio_y_precio_negativo_en_constructor_directo() {
        // When / Then — guardas defensivas en el constructor directo
        assertThatThrownBy(() ->
            new ExtraPedido(ProductoId.generate(), "  ", new BigDecimal("100"))
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("nombre");

        assertThatThrownBy(() ->
            new ExtraPedido(ProductoId.generate(), "Queso", new BigDecimal("-1"))
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("negativo");
    }
}
