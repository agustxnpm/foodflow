package com.agustinpalma.comandas.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Entidad JPA para persistencia de locales (tenants).
 */
@Entity
@Table(name = "locales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocalEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "nombre", nullable = false)
    private String nombre;
}
