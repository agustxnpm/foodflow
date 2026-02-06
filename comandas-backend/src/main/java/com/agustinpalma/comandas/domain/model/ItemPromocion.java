package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad que representa un ítem dentro del alcance de una promoción.
 * Pertenece al aggregate Promocion (parte de AlcancePromocion).
 * 
 * HU-09: Asociar productos a promociones.
 * 
 * Define:
 * - **Qué** producto/categoría participa (referenciaId)
 * - **Cómo** participa (tipo: PRODUCTO o CATEGORIA)
 * - **Con qué rol** (TRIGGER activa, TARGET recibe beneficio)
 * 
 * Invariantes:
 * - referenciaId obligatorio
 * - tipo obligatorio
 * - rol obligatorio
 * 
 * Inmutable después de la construcción.
 */
public class ItemPromocion {

    private final ItemPromocionId id;
    private final UUID referenciaId; // ProductoId o CategoriaId
    private final TipoAlcance tipo;
    private final RolPromocion rol;

    public ItemPromocion(
            ItemPromocionId id,
            UUID referenciaId,
            TipoAlcance tipo,
            RolPromocion rol
    ) {
        this.id = Objects.requireNonNull(id, "El id del item de promoción no puede ser null");
        this.referenciaId = Objects.requireNonNull(referenciaId, "La referenciaId no puede ser null");
        this.tipo = Objects.requireNonNull(tipo, "El tipo de alcance no puede ser null");
        this.rol = Objects.requireNonNull(rol, "El rol de promoción no puede ser null");
    }

    /**
     * Factory method para crear un ítem de tipo PRODUCTO con rol TRIGGER.
     */
    public static ItemPromocion productoTrigger(UUID productoId) {
        return new ItemPromocion(
                ItemPromocionId.generate(),
                productoId,
                TipoAlcance.PRODUCTO,
                RolPromocion.TRIGGER
        );
    }

    /**
     * Factory method para crear un ítem de tipo PRODUCTO con rol TARGET.
     */
    public static ItemPromocion productoTarget(UUID productoId) {
        return new ItemPromocion(
                ItemPromocionId.generate(),
                productoId,
                TipoAlcance.PRODUCTO,
                RolPromocion.TARGET
        );
    }

    /**
     * Factory method para crear un ítem de tipo CATEGORIA con rol TARGET.
     */
    public static ItemPromocion categoriaTarget(UUID categoriaId) {
        return new ItemPromocion(
                ItemPromocionId.generate(),
                categoriaId,
                TipoAlcance.CATEGORIA,
                RolPromocion.TARGET
        );
    }

    public ItemPromocionId getId() {
        return id;
    }

    public UUID getReferenciaId() {
        return referenciaId;
    }

    public TipoAlcance getTipo() {
        return tipo;
    }

    public RolPromocion getRol() {
        return rol;
    }

    public boolean esTrigger() {
        return rol == RolPromocion.TRIGGER;
    }

    public boolean esTarget() {
        return rol == RolPromocion.TARGET;
    }

    public boolean esProducto() {
        return tipo == TipoAlcance.PRODUCTO;
    }

    public boolean esCategoria() {
        return tipo == TipoAlcance.CATEGORIA;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemPromocion that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "ItemPromocion{" +
                "id=" + id +
                ", referenciaId=" + referenciaId +
                ", tipo=" + tipo +
                ", rol=" + rol +
                '}';
    }
}
