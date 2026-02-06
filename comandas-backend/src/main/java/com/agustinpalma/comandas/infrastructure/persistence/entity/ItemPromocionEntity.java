package com.agustinpalma.comandas.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;

import com.agustinpalma.comandas.domain.model.DomainEnums.RolPromocion;
import com.agustinpalma.comandas.domain.model.DomainEnums.TipoAlcance;

/**
 * Entidad JPA para la tabla intermedia 'promocion_productos_scope'.
 * 
 * HU-09: Asociar productos a promociones.
 * 
 * Representa un ítem dentro del alcance de una promoción, definiendo:
 * - Qué producto/categoría participa (referenciaId)
 * - Con qué tipo (PRODUCTO o CATEGORIA)
 * - Con qué rol (TRIGGER activa, TARGET recibe beneficio)
 */
@Entity
@Table(name = "promocion_productos_scope")
public class ItemPromocionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "promocion_id", nullable = false)
    private UUID promocionId;

    @Column(name = "referencia_id", nullable = false)
    private UUID referenciaId;

    @Column(name = "tipo_alcance", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TipoAlcance tipoAlcance;

    @Column(name = "rol", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RolPromocion rol;

    // Constructor sin argumentos requerido por JPA
    protected ItemPromocionEntity() {
    }

    public ItemPromocionEntity(
            UUID id,
            UUID promocionId,
            UUID referenciaId,
            TipoAlcance tipoAlcance,
            RolPromocion rol
    ) {
        this.id = id;
        this.promocionId = promocionId;
        this.referenciaId = referenciaId;
        this.tipoAlcance = tipoAlcance;
        this.rol = rol;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPromocionId() {
        return promocionId;
    }

    public void setPromocionId(UUID promocionId) {
        this.promocionId = promocionId;
    }

    public UUID getReferenciaId() {
        return referenciaId;
    }

    public void setReferenciaId(UUID referenciaId) {
        this.referenciaId = referenciaId;
    }

    public TipoAlcance getTipoAlcance() {
        return tipoAlcance;
    }

    public void setTipoAlcance(TipoAlcance tipoAlcance) {
        this.tipoAlcance = tipoAlcance;
    }

    public RolPromocion getRol() {
        return rol;
    }

    public void setRol(RolPromocion rol) {
        this.rol = rol;
    }
}
