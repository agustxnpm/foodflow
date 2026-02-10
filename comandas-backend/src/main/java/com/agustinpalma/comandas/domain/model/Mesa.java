package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoMesa;
import java.util.Objects;

public class Mesa {

    private final MesaId id;
    private final LocalId localId;
    private final int numero;
    private EstadoMesa estado;

    public Mesa(MesaId id, LocalId localId, int numero) {
        this.id = Objects.requireNonNull(id, "El id de la mesa es obligatorio");
        this.localId = Objects.requireNonNull(localId, "El localId es obligatorio");
        this.numero = validarNumero(numero);
        this.estado = EstadoMesa.LIBRE;
    }

    // --- Comportamiento de Negocio ---

    /**
     * Transiciona la mesa de LIBRE a ABIERTA.
     * Ocurre cuando se asigna un nuevo pedido a la mesa.
     */
    public void abrir() {
        if (this.estado == EstadoMesa.ABIERTA) {
            throw new IllegalArgumentException("La mesa " + numero + " ya se encuentra abierta.");
        }
        this.estado = EstadoMesa.ABIERTA;
    }

    /**
     * Transiciona la mesa de ABIERTA a LIBRE.
     * Ocurre cuando el pedido asociado se cierra o se libera la mesa.
     */
    public void liberar() {
        if (this.estado == EstadoMesa.LIBRE) {
            throw new IllegalArgumentException("La mesa " + numero + " ya está libre.");
        }
        this.estado = EstadoMesa.LIBRE;
    }

    /**
     * Alias de retrocompatibilidad para {@link #liberar()}.
     * @deprecated Usar {@link #liberar()} en su lugar
     */
    public void cerrar() {
        liberar();
    }

    private int validarNumero(int numero) {
        if (numero <= 0) {
            throw new IllegalArgumentException("El número de mesa debe ser positivo");
        }
        return numero;
    }

   // --- Getters ---

    public MesaId getId() {
        return id;
    }

    public LocalId getLocalId() {
        return localId;
    }

    public int getNumero() {
        return numero;
    }

    public EstadoMesa getEstado() {
        return estado;
    }

    // --- Identity ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mesa mesa = (Mesa) o;
        return Objects.equals(id, mesa.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}