package com.agustinpalma.comandas.testutil;

import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import com.agustinpalma.comandas.domain.model.Producto;

import java.math.BigDecimal;

/**
 * Builder fluido para construir un {@link Producto} válido en tests.
 *
 * Usa el constructor de retrocompatibilidad de {@link Producto} (sin variantes,
 * sin extras, sin control de stock). Para escenarios que requieran control
 * de stock activado, instanciar {@link Producto} directamente con el constructor completo.
 *
 * Uso típico:
 * <pre>
 *     Producto pizza = ProductoTestBuilder.conPrecio("1500")
 *         .enLocal(localId)
 *         .conNombre("Pizza Muzzarella")
 *         .build();
 * </pre>
 */
public final class ProductoTestBuilder {

    private LocalId    localId  = LocalId.generate();
    private BigDecimal precio   = new BigDecimal("1000");
    private String     nombre   = "Producto Test";
    private boolean    activo   = true;
    private String     colorHex = "#FFFFFF";

    private ProductoTestBuilder() { }

    /** Builder con un precio inicial expresado como string para evitar ambigüedad. */
    public static ProductoTestBuilder conPrecio(String precio) {
        ProductoTestBuilder b = new ProductoTestBuilder();
        b.precio = new BigDecimal(precio);
        return b;
    }

    /** Builder con todos los valores por defecto. */
    public static ProductoTestBuilder nuevo() {
        return new ProductoTestBuilder();
    }

    public ProductoTestBuilder enLocal(LocalId localId) {
        this.localId = localId;
        return this;
    }

    public ProductoTestBuilder conNombre(String nombre) {
        this.nombre = nombre;
        return this;
    }

    public ProductoTestBuilder conColor(String colorHex) {
        this.colorHex = colorHex;
        return this;
    }

    public ProductoTestBuilder inactivo() {
        this.activo = false;
        return this;
    }

    public Producto build() {
        return new Producto(
            ProductoId.generate(),
            localId,
            nombre,
            precio,
            activo,
            colorHex
        );
    }
}
