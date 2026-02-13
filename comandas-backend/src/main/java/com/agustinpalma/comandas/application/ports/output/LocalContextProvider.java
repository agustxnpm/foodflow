package com.agustinpalma.comandas.application.ports.output;

import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;

/**
 * Puerto de salida para obtener el LocalId del contexto operativo actual.
 *
 * Abstracción que desacopla la resolución del tenant de la fuente concreta:
 * - En single-tenant on-premise: configuración fija desde application.yml
 * - En multi-tenant SaaS (futuro): extracción desde JWT/sesión
 *
 * Los controllers y casos de uso dependen de este puerto,
 * nunca de la configuración directamente.
 */
public interface LocalContextProvider {

    /**
     * Obtiene el LocalId correspondiente a la operación actual.
     *
     * @return el LocalId configurado o resuelto del contexto
     */
    LocalId getCurrentLocalId();
}
