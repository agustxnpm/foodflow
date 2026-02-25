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
    private ProductoId grupoVarianteId;            // Identifica productos hermanos (misma variante) — mutable para asignación tardía
    private boolean esExtra;                        // true si es un extra (huevo, queso, disco, etc.) — reclasificable
    private boolean esModificadorEstructural;       // true si este extra activa normalización de variantes al agregarse
    private Integer cantidadDiscosCarne;            // Define jerarquía de variantes (null si no aplica) — mutable para asignación tardía

    // Clasificación de catálogo
    private CategoriaId categoriaId;                // FK a la categoría del catálogo — nullable para retrocompatibilidad
    private boolean permiteExtras;                  // Si false, el POS oculta la sección de extras

    // Control de flujo POS
    private boolean requiereConfiguracion;          // Si true, el POS abre modal de configuración antes de agregar al pedido

    // HU-22: Gestión de stock
    private int stockActual;                       // Cantidad actual en inventario (puede ser negativo por flexibilidad operativa)
    private boolean controlaStock;                 // Si es false, las operaciones de stock no tienen efecto

    /**
     * Constructor completo con soporte para variantes, extras y modificadores estructurales.
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
            boolean esModificadorEstructural,
            Integer cantidadDiscosCarne,
            CategoriaId categoriaId,
            boolean permiteExtras,
            boolean requiereConfiguracion,
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
        this.esModificadorEstructural = esModificadorEstructural;
        this.cantidadDiscosCarne = cantidadDiscosCarne;  // Puede ser null si no aplica
        this.categoriaId = categoriaId;  // Puede ser null para retrocompatibilidad
        this.permiteExtras = permiteExtras;
        this.requiereConfiguracion = requiereConfiguracion;
        this.stockActual = stockActual;
        this.controlaStock = controlaStock;
    }
    
    /**
     * Constructor de retrocompatibilidad (sin variantes ni extras).
     * Usado por tests y código legacy.
     */
    public Producto(ProductoId id, LocalId localId, String nombre, BigDecimal precio, boolean activo, String colorHex) {
        this(id, localId, nombre, precio, activo, colorHex, null, false, false, null, null, true, true, 0, false);
    }

    /**
     * Constructor de retrocompatibilidad con variantes (sin stock).
     * Usado por tests de variantes existentes.
     */
    public Producto(ProductoId id, LocalId localId, String nombre, BigDecimal precio, boolean activo, String colorHex,
                    ProductoId grupoVarianteId, boolean esExtra, Integer cantidadDiscosCarne) {
        this(id, localId, nombre, precio, activo, colorHex, grupoVarianteId, esExtra, false, cantidadDiscosCarne, null, true, true, 0, false);
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

    /**
     * Reclasifica el producto como extra o como producto regular.
     * Un extra es un complemento (ej: huevo, queso, disco de carne)
     * que se agrega a otros productos.
     *
     * Nota: esto NO afecta ítems ya snapshotados en pedidos abiertos.
     */
    public void reclasificarExtra(boolean esExtra) {
        this.esExtra = esExtra;
    }

    /**
     * Indica si este extra es un modificador estructural.
     * Un modificador estructural activa la normalización de variantes
     * al ser agregado como extra (ej: agregar un disco de carne a una
     * hamburguesa simple la convierte en doble).
     *
     * Solo tiene sentido semántico cuando esExtra == true.
     */
    public boolean isEsModificadorEstructural() {
        return esModificadorEstructural;
    }

    /**
     * Cambia si el producto es un modificador estructural.
     */
    public void cambiarModificadorEstructural(boolean esModificadorEstructural) {
        this.esModificadorEstructural = esModificadorEstructural;
    }
    
    public Integer getCantidadDiscosCarne() {
        return cantidadDiscosCarne;
    }

    /**
     * Asigna este producto a un grupo de variantes.
     * Solo puede llamarse si el producto NO tiene grupo asignado todavía.
     * 
     * Caso de uso principal: cuando se crea la primera variante de un producto,
     * el producto base necesita ser incorporado al grupo retroactivamente.
     * 
     * @param grupoId identificador del grupo de variantes (típicamente el ID del producto base)
     * @param cantidadDiscos posición jerárquica de este producto dentro del grupo (ej: 1=Simple)
     * @throws IllegalStateException si el producto ya pertenece a un grupo de variantes
     * @throws IllegalArgumentException si grupoId es null
     */
    public void asignarGrupoVariante(ProductoId grupoId, Integer cantidadDiscos) {
        if (this.grupoVarianteId != null) {
            throw new IllegalStateException(
                "El producto '" + this.nombre + "' ya pertenece al grupo de variantes: " + this.grupoVarianteId.getValue()
            );
        }
        this.grupoVarianteId = Objects.requireNonNull(grupoId, "El grupoVarianteId no puede ser null");
        this.cantidadDiscosCarne = cantidadDiscos;
    }
    
    /**
     * Indica si este producto pertenece a un grupo de variantes estructurales.
     * 
     * Un producto tiene variantes estructurales si pertenece a una familia
     * de productos agrupados por grupoVarianteId (ej: Simple, Doble, Triple
     * de una misma línea). El criterio es genérico y NO depende de conceptos
     * específicos de comida (discos, ingredientes, etc.).
     * 
     * Este método reemplaza a tieneVariantes() y esVarianteEstructural()
     * unificando el concepto: si tiene grupoVarianteId, participa en
     * normalización de variantes.
     */
    public boolean tieneVariantesEstructurales() {
        return grupoVarianteId != null;
    }

    // ============================================
    // Clasificación de catálogo
    // ============================================

    /**
     * Referencia a la categoría del catálogo a la que pertenece este producto.
     * Puede ser null si el producto no está clasificado (retrocompatibilidad).
     */
    public CategoriaId getCategoriaId() {
        return categoriaId;
    }

    /**
     * Actualiza la categoría del producto.
     *
     * @param categoriaId nueva categoría (puede ser null para desclasificar)
     */
    public void actualizarCategoria(CategoriaId categoriaId) {
        this.categoriaId = categoriaId;
    }

    /**
     * Indica si este producto acepta extras/agregados.
     * Si es false, el POS oculta la sección de extras en el modal de configuración.
     * Ejemplo: las bebidas típicamente no aceptan extras.
     */
    public boolean isPermiteExtras() {
        return permiteExtras;
    }

    /**
     * Cambia si el producto acepta extras/agregados.
     */
    public void cambiarPermiteExtras(boolean permiteExtras) {
        this.permiteExtras = permiteExtras;
    }

    /**
     * Indica si el POS debe abrir el modal de configuración (observaciones + extras)
     * antes de agregar este producto al pedido.
     * Si es false, el producto se agrega directamente con un solo toque.
     */
    public boolean isRequiereConfiguracion() {
        return requiereConfiguracion;
    }

    /**
     * Cambia si el producto requiere configuración en el POS.
     * Permite al operador decidir qué productos abren modal de extras/observaciones.
     */
    public void cambiarRequiereConfiguracion(boolean requiereConfiguracion) {
        this.requiereConfiguracion = requiereConfiguracion;
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
