package com.agustinpalma.comandas.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.UUID;

/**
 * Configuración tipada para el contexto operativo del sistema.
 *
 * Lee las propiedades bajo el prefijo "app.context" de application.yml.
 * Es el único punto donde se lee la configuración del LocalId.
 */
@ConfigurationProperties(prefix = "app.context")
public class MeisenProperties {

    private UUID localId;

    public UUID getLocalId() {
        return localId;
    }

    public void setLocalId(UUID localId) {
        this.localId = localId;
    }
}
