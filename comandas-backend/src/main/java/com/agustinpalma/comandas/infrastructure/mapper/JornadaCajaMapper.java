package com.agustinpalma.comandas.infrastructure.mapper;

import com.agustinpalma.comandas.domain.model.DomainIds.JornadaCajaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.JornadaCaja;
import com.agustinpalma.comandas.infrastructure.persistence.entity.JornadaCajaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper entre entidades de dominio JornadaCaja y entidades JPA JornadaCajaEntity.
 * Actúa como anti-corruption layer, protegiendo el dominio de detalles de persistencia.
 */
@Component
public class JornadaCajaMapper {

    /**
     * Convierte de entidad JPA a entidad de dominio.
     * Usa el constructor de reconstrucción que no recalcula la fecha operativa.
     *
     * @param entity entidad JPA
     * @return entidad de dominio reconstruida
     */
    public JornadaCaja toDomain(JornadaCajaEntity entity) {
        if (entity == null) {
            return null;
        }

        return new JornadaCaja(
            new JornadaCajaId(entity.getId()),
            new LocalId(entity.getLocalId()),
            entity.getFechaOperativa(),
            entity.getFechaCierre(),
            entity.getTotalVentasReales(),
            entity.getTotalConsumoInterno(),
            entity.getTotalEgresos(),
            entity.getBalanceEfectivo(),
            entity.getPedidosCerradosCount(),
            entity.getEstado()
        );
    }

    /**
     * Convierte de entidad de dominio a entidad JPA.
     *
     * @param jornada entidad de dominio
     * @return entidad JPA para persistencia
     */
    public JornadaCajaEntity toEntity(JornadaCaja jornada) {
        if (jornada == null) {
            return null;
        }

        return new JornadaCajaEntity(
            jornada.getId().getValue(),
            jornada.getLocalId().getValue(),
            jornada.getFechaOperativa(),
            jornada.getFechaCierre(),
            jornada.getTotalVentasReales(),
            jornada.getTotalConsumoInterno(),
            jornada.getTotalEgresos(),
            jornada.getBalanceEfectivo(),
            jornada.getPedidosCerradosCount(),
            jornada.getEstado()
        );
    }
}
