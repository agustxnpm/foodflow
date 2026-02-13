package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainIds.*;
import java.util.Objects;

/**
 * Entidad que representa un usuario operador del local.
 * Pertenece al aggregate Local.
 */
public class Usuario {

    private final UsuarioId id;
    private final LocalId localId;
    private String email;
    private String passwordHash;
    private boolean activo;

    public Usuario(UsuarioId id, LocalId localId, String email, String passwordHash, boolean activo) {
        this.id = Objects.requireNonNull(id, "El id del usuario no puede ser null");
        this.localId = Objects.requireNonNull(localId, "El localId no puede ser null");
        this.email = validarEmail(email);
        this.passwordHash = Objects.requireNonNull(passwordHash, "El passwordHash no puede ser null");
        this.activo = activo;
    }

    private String validarEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El email no puede estar vacío");
        }
        // Validación básica de formato
        if (!email.contains("@")) {
            throw new IllegalArgumentException("El email debe tener un formato válido");
        }
        return email.trim().toLowerCase();
    }

    public UsuarioId getId() {
        return id;
    }

    public LocalId getLocalId() {
        return localId;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActivo() {
        return activo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Usuario usuario = (Usuario) o;
        return Objects.equals(id, usuario.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
