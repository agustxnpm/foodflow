package com.agustinpalma.comandas.infrastructure.mapper;

import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.infrastructure.persistence.entity.MesaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper entre entidades de dominio y entidades JPA.
 * Actúa como anti-corruption layer, protegiendo el dominio de detalles de persistencia.
 */
@Component
public class MesaMapper {

    /**
     * Convierte de entidad JPA a entidad de dominio.
     *
     * @param entity entidad JPA
     * @return entidad de dominio reconstruida
     */
    public Mesa toDomain(MesaEntity entity) {
        if (entity == null) {
            return null;
        }

        Mesa mesa = new Mesa(
            new MesaId(entity.getId()),
            new LocalId(entity.getLocalId()),
            entity.getNumero()
        );

        // Reconstituir el estado (forzar transición si es necesario)
        if (entity.getEstado() == com.agustinpalma.comandas.domain.model.DomainEnums.EstadoMesa.ABIERTA) {
            mesa.abrir();
        }

        return mesa;
    }

    /**
     * Convierte de entidad de dominio a entidad JPA.
     *
     * @param mesa entidad de dominio
     * @return entidad JPA para persistencia
     */
    public MesaEntity toEntity(Mesa mesa) {
        if (mesa == null) {
            return null;
        }

        return new MesaEntity(
            mesa.getId().getValue(),
            mesa.getLocalId().getValue(),
            mesa.getNumero(),
            mesa.getEstado()
        );
    }
}
