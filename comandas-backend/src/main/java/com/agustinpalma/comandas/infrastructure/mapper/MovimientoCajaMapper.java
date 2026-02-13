package com.agustinpalma.comandas.infrastructure.mapper;

import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MovimientoCajaId;
import com.agustinpalma.comandas.domain.model.MovimientoCaja;
import com.agustinpalma.comandas.infrastructure.persistence.entity.MovimientoCajaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper entre entidades de dominio MovimientoCaja y entidades JPA MovimientoCajaEntity.
 * Actúa como anti-corruption layer, protegiendo el dominio de detalles de persistencia.
 */
@Component
public class MovimientoCajaMapper {

    /**
     * Convierte de entidad JPA a entidad de dominio.
     * Usa el constructor de reconstrucción que no genera nuevo comprobante.
     *
     * @param entity entidad JPA
     * @return entidad de dominio reconstruida
     */
    public MovimientoCaja toDomain(MovimientoCajaEntity entity) {
        if (entity == null) {
            return null;
        }

        return new MovimientoCaja(
            new MovimientoCajaId(entity.getId()),
            new LocalId(entity.getLocalId()),
            entity.getMonto(),
            entity.getDescripcion(),
            entity.getFecha(),
            entity.getTipo(),
            entity.getNumeroComprobante()
        );
    }

    /**
     * Convierte de entidad de dominio a entidad JPA.
     *
     * @param movimiento entidad de dominio
     * @return entidad JPA para persistencia
     */
    public MovimientoCajaEntity toEntity(MovimientoCaja movimiento) {
        if (movimiento == null) {
            return null;
        }

        return new MovimientoCajaEntity(
            movimiento.getId().getValue(),
            movimiento.getLocalId().getValue(),
            movimiento.getMonto(),
            movimiento.getDescripcion(),
            movimiento.getFecha(),
            movimiento.getTipo(),
            movimiento.getNumeroComprobante()
        );
    }
}
