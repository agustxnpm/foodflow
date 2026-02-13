package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainIds.*;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Entidad que representa un producto del catálogo del local.
 * Pertenece al aggregate Local.
 * 
 * Reglas de negocio:
 * - El nombre debe ser único dentro del local (case insensitive)
 * - El precio debe ser positivo (> 0)
 * - El colorHex debe seguir formato hexadecimal válido (#RGB o #RRGGBB)
 * - El colorHex se normaliza siempre a mayúsculas para consistencia
 * - La edición del precio NO afecta pedidos ya abiertos (garantizado por patrón Snapshot en ItemPedido)
 * 
 * HU-05.1 + HU-22: Soporte para variantes y extras controlados
 * - Los productos pueden pertenecer a un grupo de variantes (ej: Hamburguesa Simple, Doble, Triple)
 * - grupoVarianteId: Identifica variantes del mismo concepto (ej: todas las hamburguesas "Completa")
 * - cantidadDiscosCarne: Define la jerarquía de variantes (Simple=1, Doble=2, Triple=3)
 * - esExtra: Indica si el producto es un extra (ej: huevo, queso, disco de carne)
 * 
 * Regla crítica de normalización:
 * El disco de carne SOLO puede agregarse como extra a la variante máxima de su grupo.
 * Si se intenta agregar a una variante menor, el sistema convierte automáticamente al siguiente nivel.
 */
public class Producto {

    private static final String COLOR_HEX_DEFAULT = "#FFFFFF";
    private static final Pattern COLOR_HEX_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");

    private final ProductoId id;
    private final LocalId localId;
    private String nombre;
    private BigDecimal precio;
    private boolean activo;
    private String colorHex;
    
    // HU-05.1 + HU-22: Soporte para variantes y extras
    private final ProductoId grupoVarianteId;      // Identifica productos hermanos (misma variante)
    private final boolean esExtra;                  // true si es un extra (huevo, queso, disco, etc.)
    private final Integer cantidadDiscosCarne;     // Define jerarquía de variantes (null si no aplica)

    // HU-22: Gestión de stock
    private int stockActual;                       // Cantidad actual en inventario (puede ser negativo por flexibilidad operativa)
    private boolean controlaStock;                 // Si es false, las operaciones de stock no tienen efecto

    /**
     * Constructor completo con soporte para variantes y extras (HU-05.1 + HU-22).
     */
    public Producto(
            ProductoId id, 
            LocalId localId, 
            String nombre, 
            BigDecimal precio, 
            boolean activo, 
            String colorHex,
            ProductoId grupoVarianteId,
            boolean esExtra,
            Integer cantidadDiscosCarne,
            int stockActual,
            boolean controlaStock
    ) {
        this.id = Objects.requireNonNull(id, "El id del producto no puede ser null");
        this.localId = Objects.requireNonNull(localId, "El localId no puede ser null");
        this.nombre = validarNombre(nombre);
        this.precio = validarPrecio(precio);
        this.activo = activo;
        this.colorHex = normalizarColor(colorHex);
        this.grupoVarianteId = grupoVarianteId;  // Puede ser null si no tiene variantes
        this.esExtra = esExtra;
        this.cantidadDiscosCarne = cantidadDiscosCarne;  // Puede ser null si no aplica
        this.stockActual = stockActual;
        this.controlaStock = controlaStock;
    }
    
    /**
     * Constructor de retrocompatibilidad (sin variantes ni extras).
     * Usado por tests y código legacy.
     */
    public Producto(ProductoId id, LocalId localId, String nombre, BigDecimal precio, boolean activo, String colorHex) {
        this(id, localId, nombre, precio, activo, colorHex, null, false, null, 0, false);
    }

    /**
     * Constructor de retrocompatibilidad con variantes (sin stock).
     * Usado por tests de variantes existentes.
     */
    public Producto(ProductoId id, LocalId localId, String nombre, BigDecimal precio, boolean activo, String colorHex,
                    ProductoId grupoVarianteId, boolean esExtra, Integer cantidadDiscosCarne) {
        this(id, localId, nombre, precio, activo, colorHex, grupoVarianteId, esExtra, cantidadDiscosCarne, 0, false);
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del producto no puede estar vacío");
        }
        return nombre.trim();
    }

    private BigDecimal validarPrecio(BigDecimal precio) {
        if (precio == null) {
            throw new IllegalArgumentException("El precio no puede ser null");
        }
        if (precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero");
        }
        return precio;
    }

    /**
     * Normaliza el color hexadecimal.
     * Si es null, asigna el color por defecto (blanco).
     * Si no cumple el formato válido, lanza excepción.
     * Siempre lo convierte a mayúsculas para consistencia.
     */
    private String normalizarColor(String color) {
        if (color == null || color.isBlank()) {
            return COLOR_HEX_DEFAULT;
        }
        
        String colorTrimmed = color.trim();
        if (!COLOR_HEX_PATTERN.matcher(colorTrimmed).matches()) {
            throw new IllegalArgumentException(
                "El color debe seguir formato hexadecimal válido (#RGB o #RRGGBB). Recibido: " + color
            );
        }
        
        return colorTrimmed.toUpperCase();
    }

    /**
     * Actualiza el nombre del producto.
     * Valida que no esté vacío.
     */
    public void actualizarNombre(String nuevoNombre) {
        this.nombre = validarNombre(nuevoNombre);
    }

    /**
     * Actualiza el precio del producto.
     * Valida que sea positivo.
     * Nota: Esta actualización NO afecta ítems en pedidos ya abiertos
     * (garantizado por el patrón Snapshot en ItemPedido).
     */
    public void actualizarPrecio(BigDecimal nuevoPrecio) {
        this.precio = validarPrecio(nuevoPrecio);
    }

    /**
     * Cambia el estado activo/inactivo del producto.
     */
    public void cambiarEstado(boolean nuevoEstado) {
        this.activo = nuevoEstado;
    }

    /**
     * Actualiza el color del producto.
     * Valida formato hexadecimal y normaliza a mayúsculas.
     */
    public void actualizarColor(String nuevoColor) {
        this.colorHex = normalizarColor(nuevoColor);
    }

    public ProductoId getId() {
        return id;
    }

    public LocalId getLocalId() {
        return localId;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public boolean isActivo() {
        return activo;
    }

    public String getColorHex() {
        return colorHex;
    }
    
    // HU-05.1 + HU-22: Getters para variantes y extras
    
    public ProductoId getGrupoVarianteId() {
        return grupoVarianteId;
    }
    
    public boolean isEsExtra() {
        return esExtra;
    }
    
    public Integer getCantidadDiscosCarne() {
        return cantidadDiscosCarne;
    }
    
    /**
     * Indica si este producto pertenece a un grupo de variantes.
     */
    public boolean tieneVariantes() {
        return grupoVarianteId != null;
    }
    
    /**
     * Indica si este producto es una hamburguesa (tiene discos de carne definidos).
     */
    public boolean esHamburguesa() {
        return cantidadDiscosCarne != null && cantidadDiscosCarne > 0;
    }

    // ============================================
    // HU-22: Gestión de stock
    // ============================================

    public int getStockActual() {
        return stockActual;
    }

    public boolean isControlaStock() {
        return controlaStock;
    }

    /**
     * Descuenta stock del producto.
     * Si el producto no controla stock, la operación no tiene efecto.
     * Se permite stock negativo por flexibilidad operativa.
     *
     * @param cantidad cantidad a descontar (debe ser > 0)
     * @throws IllegalArgumentException si la cantidad es <= 0
     */
    public void descontarStock(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a descontar debe ser mayor a cero");
        }
        if (!controlaStock) {
            return;
        }
        this.stockActual -= cantidad;
    }

    /**
     * Repone stock del producto.
     * Si el producto no controla stock, la operación no tiene efecto.
     *
     * @param cantidad cantidad a reponer (debe ser > 0)
     * @throws IllegalArgumentException si la cantidad es <= 0
     */
    public void reponerStock(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a reponer debe ser mayor a cero");
        }
        if (!controlaStock) {
            return;
        }
        this.stockActual += cantidad;
    }

    /**
     * Activa el control de stock para este producto.
     */
    public void activarControlStock() {
        this.controlaStock = true;
    }

    /**
     * Desactiva el control de stock para este producto.
     */
    public void desactivarControlStock() {
        this.controlaStock = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Producto producto = (Producto) o;
        return Objects.equals(id, producto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
