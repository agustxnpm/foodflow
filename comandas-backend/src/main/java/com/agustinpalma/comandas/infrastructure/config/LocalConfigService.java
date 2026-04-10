package com.agustinpalma.comandas.infrastructure.config;

import com.agustinpalma.comandas.infrastructure.persistence.entity.LocalConfigEntity;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataLocalConfigRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Servicio que encapsula la lectura de datos del local desde la tabla local_config.
 *
 * Reemplaza a MeisenProperties.LocalProperties como fuente de datos del local.
 * Lee toda la configuración una vez al iniciar y la mantiene en memoria.
 *
 * Resiliente: si una clave no existe, devuelve string vacío.
 */
@Service
public class LocalConfigService {

    private static final Logger log = LoggerFactory.getLogger(LocalConfigService.class);

    private static final String KEY_NOMBRE_LOCAL = "nombre-local";
    private static final String KEY_DIRECCION = "direccion";
    private static final String KEY_TELEFONO = "telefono";
    private static final String KEY_CUIT = "cuit";
    private static final String KEY_MENSAJE_BIENVENIDA = "mensaje-bienvenida";

    private final SpringDataLocalConfigRepository configRepository;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public LocalConfigService(SpringDataLocalConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @PostConstruct
    void cargarConfiguracion() {
        recargar();
    }

    /**
     * Recarga la configuración desde la base de datos.
     * Se invoca al iniciar y puede invocarse después de la activación.
     */
    public void recargar() {
        Map<String, String> datos = configRepository.findAll().stream()
                .collect(Collectors.toMap(
                        LocalConfigEntity::getKey,
                        LocalConfigEntity::getValue,
                        (v1, v2) -> v2
                ));
        cache.clear();
        cache.putAll(datos);

        if (datos.isEmpty()) {
            log.info("LocalConfigService: sin configuración en DB (pendiente de activación)");
        } else {
            log.info("LocalConfigService: configuración cargada ({} claves)", datos.size());
        }
    }

    public String getNombreLocal() {
        return cache.getOrDefault(KEY_NOMBRE_LOCAL, "");
    }

    public String getDireccion() {
        return cache.getOrDefault(KEY_DIRECCION, "");
    }

    public String getTelefono() {
        return cache.getOrDefault(KEY_TELEFONO, "");
    }

    public String getCuit() {
        return cache.getOrDefault(KEY_CUIT, "");
    }

    public String getMensajeBienvenida() {
        return cache.getOrDefault(KEY_MENSAJE_BIENVENIDA, "¡Gracias por su visita!");
    }
}
