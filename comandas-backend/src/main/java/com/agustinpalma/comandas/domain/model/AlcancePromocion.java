package com.agustinpalma.comandas.domain.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Value Object que representa el alcance (scope) de una promoción.
 * 
 * HU-09: Asociar productos a promociones.
 * 
 * Define:
 * - **Qué productos/categorías ACTIVAN** la promoción (rol TRIGGER)
 * - **Qué productos/categorías RECIBEN el beneficio** (rol TARGET)
 * 
 * Reglas de negocio:
 * - Puede tener 0 o más items TRIGGER
 * - Puede tener 0 o más items TARGET
 * - Un mismo producto puede ser TRIGGER en una promo y TARGET en otra
 * - Dentro de la misma promoción, un producto NO puede ser TRIGGER y TARGET simultáneamente
 * 
 * Inmutable, construido vía lista de ItemPromocion.
 */
public final class AlcancePromocion {

    private final List<ItemPromocion> items;

    public AlcancePromocion(List<ItemPromocion> items) {
        this.items = items != null ? List.copyOf(items) : Collections.emptyList();
        validarNoHayDuplicados();
    }

    /**
     * Factory method para crear un alcance vacío.
     */
    public static AlcancePromocion vacio() {
        return new AlcancePromocion(Collections.emptyList());
    }

    /**
     * Valida que no haya duplicados de referenciaId dentro del mismo alcance.
     * Un producto/categoría no puede tener múltiples roles en la misma promoción.
     */
    private void validarNoHayDuplicados() {
        Set<UUID> referencias = new HashSet<>();
        for (ItemPromocion item : items) {
            if (!referencias.add(item.getReferenciaId())) {
                throw new IllegalArgumentException(
                        "El producto/categoría " + item.getReferenciaId() + 
                        " está duplicado en el alcance de la promoción"
                );
            }
        }
    }

    public List<ItemPromocion> getItems() {
        return items;
    }

    /**
     * Retorna todos los items con rol TRIGGER.
     */
    public List<ItemPromocion> getTriggers() {
        return items.stream()
                .filter(ItemPromocion::esTrigger)
                .toList();
    }

    /**
     * Retorna todos los items con rol TARGET.
     */
    public List<ItemPromocion> getTargets() {
        return items.stream()
                .filter(ItemPromocion::esTarget)
                .toList();
    }

    /**
     * Retorna todos los UUIDs de productos que son TRIGGER.
     */
    public Set<UUID> getProductosTriggerIds() {
        return items.stream()
                .filter(ItemPromocion::esTrigger)
                .filter(ItemPromocion::esProducto)
                .map(ItemPromocion::getReferenciaId)
                .collect(Collectors.toSet());
    }

    /**
     * Retorna todos los UUIDs de productos que son TARGET.
     */
    public Set<UUID> getProductosTargetIds() {
        return items.stream()
                .filter(ItemPromocion::esTarget)
                .filter(ItemPromocion::esProducto)
                .map(ItemPromocion::getReferenciaId)
                .collect(Collectors.toSet());
    }

    /**
     * Retorna todos los UUIDs de categorías que son TARGET.
     */
    public Set<UUID> getCategoriasTargetIds() {
        return items.stream()
                .filter(ItemPromocion::esTarget)
                .filter(ItemPromocion::esCategoria)
                .map(ItemPromocion::getReferenciaId)
                .collect(Collectors.toSet());
    }

    public boolean tieneItems() {
        return !items.isEmpty();
    }

    public boolean tieneTriggers() {
        return items.stream().anyMatch(ItemPromocion::esTrigger);
    }

    public boolean tieneTargets() {
        return items.stream().anyMatch(ItemPromocion::esTarget);
    }

    public int cantidadItems() {
        return items.size();
    }

    /**
     * Verifica si un producto específico está en el alcance con rol TRIGGER.
     */
    public boolean esProductoTrigger(UUID productoId) {
        return items.stream()
                .anyMatch(item -> 
                    item.getReferenciaId().equals(productoId) && 
                    item.esTrigger() && 
                    item.esProducto()
                );
    }

    /**
     * Verifica si un producto específico está en el alcance con rol TARGET.
     */
    public boolean esProductoTarget(UUID productoId) {
        return items.stream()
                .anyMatch(item -> 
                    item.getReferenciaId().equals(productoId) && 
                    item.esTarget() && 
                    item.esProducto()
                );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AlcancePromocion that)) return false;
        return Objects.equals(new HashSet<>(items), new HashSet<>(that.items));
    }

    @Override
    public int hashCode() {
        return Objects.hash(new HashSet<>(items));
    }

    @Override
    public String toString() {
        return "AlcancePromocion{" +
                "triggers=" + getTriggers().size() +
                ", targets=" + getTargets().size() +
                ", total=" + items.size() +
                '}';
    }
}
