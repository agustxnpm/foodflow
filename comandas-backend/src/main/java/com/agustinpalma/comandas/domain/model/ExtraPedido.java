package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value Object que representa un extra agregado a un ítem de pedido.
 * 
 * HU-05.1 + HU-22: Soporte para extras dinámicos (huevo, queso, bacon)
 * y extras controlados (disco de carne con validación especial).
 * 
 * Reglas de negocio:
 * - Es INMUTABLE (patrón snapshot)
 * - Captura el precio del extra AL MOMENTO de agregarlo al ítem
 * - El precio NO se modifica si cambia el catálogo (consistencia histórica)
 * - Se compara por valor (no por identidad)
 * - Los extras NUNCA participan en cálculos de promociones (aislamiento)
 * 
 * Fórmula de cálculo:
 * SubtotalLinea = cantidad × (precioUnitarioBase + sum(precioExtras))
 * 
 * Las promociones SOLO descuentan sobre precioUnitarioBase.
 */
public final class ExtraPedido {

    private final ProductoId productoId;     // ID del producto extra en catálogo (para auditoría)
    private final String nombre;              // Snapshot del nombre (ej: "Huevo", "Disco de Carne")
    private final BigDecimal precioSnapshot; // Snapshot del precio al momento de agregar

    /**
     * Constructor completo.
     * 
     * @param productoId ID del producto extra en el catálogo
     * @param nombre nombre del extra (capturado como snapshot)
     * @param precioSnapshot precio del extra al momento de agregarlo
     * @throws IllegalArgumentException si algún parámetro es inválido
     */
    public ExtraPedido(ProductoId productoId, String nombre, BigDecimal precioSnapshot) {
        this.productoId = Objects.requireNonNull(productoId, "El productoId del extra no puede ser null");
        this.nombre = validarNombre(nombre);
        this.precioSnapshot = validarPrecio(precioSnapshot);
    }

    /**
     * Factory method para crear desde un Producto del catálogo.
     * Captura automáticamente el nombre y precio actuales (snapshot).
     * 
     * @param producto entidad Producto marcada como extra (esExtra = true)
     * @return ExtraPedido con valores snapshot
     * @throws IllegalArgumentException si el producto no es un extra
     */
    public static ExtraPedido crearDesdeProducto(Producto producto) {
        Objects.requireNonNull(producto, "El producto no puede ser null");
        
        if (!producto.isEsExtra()) {
            throw new IllegalArgumentException(
                String.format("El producto '%s' no está marcado como extra (esExtra = false)", 
                    producto.getNombre())
            );
        }
        
        return new ExtraPedido(
            producto.getId(),
            producto.getNombre(),
            producto.getPrecio()
        );
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del extra no puede estar vacío");
        }
        return nombre.trim();
    }

    private BigDecimal validarPrecio(BigDecimal precio) {
        if (precio == null) {
            throw new IllegalArgumentException("El precio del extra no puede ser null");
        }
        if (precio.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio del extra no puede ser negativo");
        }
        return precio;
    }

    // Getters

    public ProductoId getProductoId() {
        return productoId;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getPrecioSnapshot() {
        return precioSnapshot;
    }

    // Value Object: comparación por valor

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtraPedido that = (ExtraPedido) o;
        return Objects.equals(productoId, that.productoId) &&
               Objects.equals(nombre, that.nombre) &&
               Objects.equals(precioSnapshot, that.precioSnapshot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productoId, nombre, precioSnapshot);
    }

    @Override
    public String toString() {
        return String.format("ExtraPedido{nombre='%s', precio=%s}", nombre, precioSnapshot);
    }
}
