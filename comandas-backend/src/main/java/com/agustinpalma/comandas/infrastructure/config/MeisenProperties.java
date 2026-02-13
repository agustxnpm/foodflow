package com.agustinpalma.comandas.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.UUID;

/**
 * Configuración tipada para el contexto operativo del sistema.
 *
 * Lee las propiedades bajo el prefijo "app.context" de application.yml.
 * Contiene:
 * - Identificación del tenant (localId)
 * - Datos de impresión del local (nombre, dirección, teléfono, CUIT, mensaje)
 *
 * Estos valores NO se hardcodean — se leen de configuración externa.
 */
@ConfigurationProperties(prefix = "app.context")
public class MeisenProperties {

    private UUID localId;
    private LocalProperties local = new LocalProperties();

    public UUID getLocalId() {
        return localId;
    }

    public void setLocalId(UUID localId) {
        this.localId = localId;
    }

    public LocalProperties getLocal() {
        return local;
    }

    public void setLocal(LocalProperties local) {
        this.local = local;
    }

    /**
     * Datos del local usados para impresión de tickets.
     * Configurables bajo "app.context.local" en application.yml.
     */
    public static class LocalProperties {

        private String nombreLocal = "";
        private String direccion = "";
        private String telefono = "";
        private String cuit = "";
        private String mensajeBienvenida = "";

        public String getNombreLocal() {
            return nombreLocal;
        }

        public void setNombreLocal(String nombreLocal) {
            this.nombreLocal = nombreLocal;
        }

        public String getDireccion() {
            return direccion;
        }

        public void setDireccion(String direccion) {
            this.direccion = direccion;
        }

        public String getTelefono() {
            return telefono;
        }

        public void setTelefono(String telefono) {
            this.telefono = telefono;
        }

        public String getCuit() {
            return cuit;
        }

        public void setCuit(String cuit) {
            this.cuit = cuit;
        }

        public String getMensajeBienvenida() {
            return mensajeBienvenida;
        }

        public void setMensajeBienvenida(String mensajeBienvenida) {
            this.mensajeBienvenida = mensajeBienvenida;
        }
    }
}
