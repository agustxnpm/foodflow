package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import java.util.Objects;

/**
 * Aggregate Root del contexto de Local.
 * Representa la configuración e identidad del tenant.
 * * NOTA: No contiene listas de recursos (Mesas, Productos) para mantener
 * el agregado pequeño y performante. La relación es inversa via local_id.
 */
public class Local {

    private final LocalId id;
    private String nombre;
    private boolean activo;

    // Constructor primario (Creación)
    public Local(LocalId id, String nombre) {
        this.id = Objects.requireNonNull(id, "El id del local es obligatorio");
        this.nombre = validarNombre(nombre);
        this.activo = true; // Por defecto nace activo
    }
    
    // Constructor para reconstitución (Persistencia)
    // Se puede usar un builder o constructor package-private si se prefiere
    public Local(LocalId id, String nombre, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.activo = activo;
    }

    // --- Comportamiento de Negocio ---

    public void renombrar(String nuevoNombre) {
        this.nombre = validarNombre(nuevoNombre);
    }

    public void reactivar() {
        this.activo = true;
    }

    // --- Validaciones Internas ---

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del local no puede estar vacío");
        }
        return nombre.trim();
    }

    // --- Getters ---

    public LocalId getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isActivo() {
        return activo;
    }

    // --- Identity ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Local local = (Local) o;
        return Objects.equals(id, local.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}