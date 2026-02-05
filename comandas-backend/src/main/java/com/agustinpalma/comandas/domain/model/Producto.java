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

    public Producto(ProductoId id, LocalId localId, String nombre, BigDecimal precio, boolean activo, String colorHex) {
        this.id = Objects.requireNonNull(id, "El id del producto no puede ser null");
        this.localId = Objects.requireNonNull(localId, "El localId no puede ser null");
        this.nombre = validarNombre(nombre);
        this.precio = validarPrecio(precio);
        this.activo = activo;
        this.colorHex = normalizarColor(colorHex);
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
