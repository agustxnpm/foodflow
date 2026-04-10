package com.agustinpalma.comandas.infrastructure.config;

import com.agustinpalma.comandas.application.ports.output.LocalContextProvider;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataLocalConfigRepository;
import org.springframework.stereotype.Component;

/**
 * Adaptador de infraestructura para resolución de LocalId en modo single-tenant.
 *
 * Resuelve el UUID del tenant desde la tabla local_config (clave "local-id"),
 * persistida durante el flujo de activación.
 *
 * El valor se cachea en memoria tras la primera resolución exitosa.
 * Si no existe el ID, lanza excepción descriptiva para que
 * el frontend redirija al flujo de activación.
 *
 * Es el ÚNICO lugar donde se accede a la identidad del tenant.
 * Controllers y casos de uso consumen el puerto {@link LocalContextProvider}.
 */
@Component
public class FixedLocalContextProvider implements LocalContextProvider {

    private static final String KEY_LOCAL_ID = "local-id";

    private final SpringDataLocalConfigRepository configRepository;
    private volatile LocalId cachedLocalId;

    public FixedLocalContextProvider(SpringDataLocalConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Override
    public LocalId getCurrentLocalId() {
        LocalId cached = this.cachedLocalId;
        if (cached != null) {
            return cached;
        }
        return resolverYCachear();
    }

    private synchronized LocalId resolverYCachear() {
        // Double-check después de adquirir el lock
        if (cachedLocalId != null) {
            return cachedLocalId;
        }

        var optConfig = configRepository.findById(KEY_LOCAL_ID);
        if (optConfig.isPresent()) {
            cachedLocalId = LocalId.from(optConfig.get().getValue());
            return cachedLocalId;
        }

        throw new IllegalStateException(
                "Local no configurado. Ejecutar activación desde la pantalla inicial."
        );
    }
}
