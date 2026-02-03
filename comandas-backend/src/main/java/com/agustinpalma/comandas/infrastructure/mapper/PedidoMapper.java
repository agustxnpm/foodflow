package com.agustinpalma.comandas.infrastructure.mapper;

import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.infrastructure.persistence.entity.PedidoEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper entre entidades de dominio Pedido y entidades JPA PedidoEntity.
 * Actúa como anti-corruption layer, protegiendo el dominio de detalles de persistencia.
 */
@Component
public class PedidoMapper {

    /**
     * Convierte de entidad JPA a entidad de dominio.
     *
     * @param entity entidad JPA
     * @return entidad de dominio reconstruida
     */
    public Pedido toDomain(PedidoEntity entity) {
        if (entity == null) {
            return null;
        }

        // Reconstruimos el pedido con sus datos base
        // Los items y descuentos se cargarán desde sus propios repositorios/mappers cuando sea necesario
        return new Pedido(
            new PedidoId(entity.getId()),
            new LocalId(entity.getLocalId()),
            new MesaId(entity.getMesaId()),
            entity.getNumero(),
            entity.getEstado(),
            entity.getFechaApertura()
        );
    }

    /**
     * Convierte de entidad de dominio a entidad JPA.
     *
     * @param pedido entidad de dominio
     * @return entidad JPA para persistencia
     */
    public PedidoEntity toEntity(Pedido pedido) {
        if (pedido == null) {
            return null;
        }

        PedidoEntity entity = new PedidoEntity(
            pedido.getId().getValue(),
            pedido.getLocalId().getValue(),
            pedido.getMesaId().getValue(),
            pedido.getNumero(),
            pedido.getEstado(),
            pedido.getFechaApertura()
        );

        entity.setFechaCierre(pedido.getFechaCierre());
        entity.setMedioPago(pedido.getMedioPago());

        return entity;
    }
}
