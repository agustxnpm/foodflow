package com.agustinpalma.comandas.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Entidad JPA para categorías del catálogo.
 * Representa la tabla de categorías en la base de datos.
 */
@Entity
@Table(name = "categorias")
public class CategoriaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "local_id", nullable = false)
    private UUID localId;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Column(name = "admite_variantes", nullable = false)
    private boolean admiteVariantes = false;

    @Column(name = "es_categoria_extra", nullable = false)
    private boolean esCategoriaExtra = false;

    @Column(name = "orden", nullable = false)
    private int orden = 0;

    // Constructor vacío para JPA
    public CategoriaEntity() {}

    // Constructor con parámetros
    public CategoriaEntity(UUID id, UUID localId, String nombre, String colorHex,
                           boolean admiteVariantes, boolean esCategoriaExtra, int orden) {
        this.id = id;
        this.localId = localId;
        this.nombre = nombre;
        this.colorHex = colorHex;
        this.admiteVariantes = admiteVariantes;
        this.esCategoriaExtra = esCategoriaExtra;
        this.orden = orden;
    }

    // Getters y setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getLocalId() {
        return localId;
    }

    public void setLocalId(UUID localId) {
        this.localId = localId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public boolean isAdmiteVariantes() {
        return admiteVariantes;
    }

    public void setAdmiteVariantes(boolean admiteVariantes) {
        this.admiteVariantes = admiteVariantes;
    }

    public boolean isEsCategoriaExtra() {
        return esCategoriaExtra;
    }

    public void setEsCategoriaExtra(boolean esCategoriaExtra) {
        this.esCategoriaExtra = esCategoriaExtra;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }
}
