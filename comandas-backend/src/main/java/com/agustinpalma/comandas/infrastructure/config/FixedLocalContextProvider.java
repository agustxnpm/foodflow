package com.agustinpalma.comandas.infrastructure.config;

import com.agustinpalma.comandas.application.ports.output.LocalContextProvider;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import org.springframework.stereotype.Component;

/**
 * Adaptador de infraestructura para resolución de LocalId en modo single-tenant.
 *
 * Lee el UUID configurado en application.yml a través de {@link MeisenProperties}
 * y lo convierte al Value Object de dominio {@link LocalId}.
 *
 * Es el ÚNICO lugar donde se accede a la configuración del tenant.
 * Controllers y casos de uso consumen el puerto {@link LocalContextProvider}.
 */
@Component
public class FixedLocalContextProvider implements LocalContextProvider {

    private final LocalId localId;

    public FixedLocalContextProvider(MeisenProperties properties) {
        if (properties.getLocalId() == null) {
            throw new IllegalStateException(
                "La propiedad 'app.context.local-id' es obligatoria. Configurar en application.yml."
            );
        }
        this.localId = new LocalId(properties.getLocalId());
    }

    @Override
    public LocalId getCurrentLocalId() {
        return localId;
    }
}
