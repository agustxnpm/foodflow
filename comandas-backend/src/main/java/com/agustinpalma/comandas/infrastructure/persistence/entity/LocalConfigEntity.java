package com.agustinpalma.comandas.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad JPA key-value para configuración de identidad del local.
 * Almacena pares clave-valor como local-id, nombre-local, cuit, etc.
 * Diseño compatible con SQLite (solo tipos String).
 */
@Entity
@Table(name = "local_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalConfigEntity {

    @Id
    @Column(name = "config_key", nullable = false)
    private String key;

    @Column(name = "config_value", nullable = false)
    private String value;
}
