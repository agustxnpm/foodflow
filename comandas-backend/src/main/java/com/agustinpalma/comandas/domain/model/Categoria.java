package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainIds.*;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Entidad de dominio que representa una categoría del catálogo del local.
 * Pertenece al aggregate Local (multi-tenant por fila).
 *
 * Las categorías agrupan productos y definen comportamiento de UI:
 * - admiteVariantes: indica si los productos de esta categoría pueden tener
 *   variantes estructurales (ej: Simple / Doble / Triple)
 * - esCategoriaExtra: indica si los productos de esta categoría son extras
 *   (ej: "Extras" → huevo, queso, disco de carne)
 *
 * Reglas de negocio:
 * - El nombre debe ser único dentro del local (case insensitive)
 * - El nombre no puede estar vacío
 * - El colorHex se normaliza a mayúsculas y se valida en formato hexadecimal
 * - El orden define la posición visual en el frontend (0 = primera posición)
 */
public class Categoria {

    private static final String COLOR_HEX_DEFAULT = "#FFFFFF";
    private static final Pattern COLOR_HEX_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");

    private final CategoriaId id;
    private final LocalId localId;
    private String nombre;
    private String colorHex;
    private boolean admiteVariantes;
    private boolean esCategoriaExtra;
    private int orden;

    /**
     * Constructor completo.
     *
     * @param id           identidad de la categoría
     * @param localId      identidad del local (tenant)
     * @param nombre       nombre de la categoría (ej: "Hamburguesas", "Bebidas")
     * @param colorHex     color hexadecimal para la UI (nullable → default #FFFFFF)
     * @param admiteVariantes si los productos de esta categoría pueden tener variantes estructurales
     * @param esCategoriaExtra si los productos de esta categoría son extras
     * @param orden        posición visual en el frontend (0-based)
     */
    public Categoria(
            CategoriaId id,
            LocalId localId,
            String nombre,
            String colorHex,
            boolean admiteVariantes,
            boolean esCategoriaExtra,
            int orden
    ) {
        this.id = Objects.requireNonNull(id, "El id de la categoría no puede ser null");
        this.localId = Objects.requireNonNull(localId, "El localId no puede ser null");
        this.nombre = validarNombre(nombre);
        this.colorHex = normalizarColor(colorHex);
        this.admiteVariantes = admiteVariantes;
        this.esCategoriaExtra = esCategoriaExtra;
        this.orden = validarOrden(orden);
    }

    // ============================================
    // Validaciones
    // ============================================

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la categoría no puede estar vacío");
        }
        return nombre.trim();
    }

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

    private int validarOrden(int orden) {
        if (orden < 0) {
            throw new IllegalArgumentException("El orden no puede ser negativo");
        }
        return orden;
    }

    // ============================================
    // Comportamiento de dominio
    // ============================================

    /**
     * Actualiza el nombre de la categoría.
     * Valida que no esté vacío y normaliza (trim).
     */
    public void actualizarNombre(String nuevoNombre) {
        this.nombre = validarNombre(nuevoNombre);
    }

    /**
     * Actualiza el color hexadecimal de la categoría.
     * Valida formato y normaliza a mayúsculas.
     */
    public void actualizarColor(String nuevoColor) {
        this.colorHex = normalizarColor(nuevoColor);
    }

    /**
     * Cambia si la categoría admite productos con variantes estructurales.
     */
    public void cambiarAdmiteVariantes(boolean admiteVariantes) {
        this.admiteVariantes = admiteVariantes;
    }

    /**
     * Cambia si la categoría es de extras.
     * Una categoría de extras contiene productos complementarios (huevo, queso, etc.).
     */
    public void cambiarEsCategoriaExtra(boolean esCategoriaExtra) {
        this.esCategoriaExtra = esCategoriaExtra;
    }

    /**
     * Cambia la posición visual de la categoría en el frontend.
     */
    public void cambiarOrden(int nuevoOrden) {
        this.orden = validarOrden(nuevoOrden);
    }

    // ============================================
    // Getters
    // ============================================

    public CategoriaId getId() {
        return id;
    }

    public LocalId getLocalId() {
        return localId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getColorHex() {
        return colorHex;
    }

    public boolean isAdmiteVariantes() {
        return admiteVariantes;
    }

    public boolean isEsCategoriaExtra() {
        return esCategoriaExtra;
    }

    public int getOrden() {
        return orden;
    }

    // ============================================
    // Identidad
    // ============================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Categoria that = (Categoria) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
