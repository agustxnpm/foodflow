package com.agustinpalma.comandas.infrastructure.mapper;

import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.ItemPedido;
import com.agustinpalma.comandas.infrastructure.persistence.entity.ItemPedidoEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper entre la entidad de dominio ItemPedido y la entidad JPA ItemPedidoEntity.
 * Actúa como anti-corruption layer, protegiendo el dominio de detalles de persistencia.
 * 
 * HU-10: Incluye mapeo de campos de promoción (montoDescuento, nombrePromocion, promocionId).
 */
@Component
public class ItemPedidoMapper {

    /**
     * Convierte de entidad JPA a entidad de dominio.
     * HU-10: Incluye campos de promoción.
     *
     * @param entity entidad JPA
     * @return entidad de dominio reconstruida
     */
    public ItemPedido toDomain(ItemPedidoEntity entity) {
        if (entity == null) {
            return null;
        }

        return new ItemPedido(
            new ItemPedidoId(entity.getId()),
            new PedidoId(entity.getPedidoId()),
            new ProductoId(entity.getProductoId()),
            entity.getNombreProducto(),
            entity.getCantidad(),
            entity.getPrecioUnitario(),
            entity.getObservacion(),
            entity.getMontoDescuento(),
            entity.getNombrePromocion(),
            entity.getPromocionId()
        );
    }

    /**
     * Convierte de entidad de dominio a entidad JPA.
     * HU-10: Incluye campos de promoción.
     * 
     * No establece la relación con PedidoEntity aquí.
     * Eso se hace en PedidoMapper.toEntity() usando el método agregarItem().
     *
     * @param domain entidad de dominio
     * @return entidad JPA para persistencia
     */
    public ItemPedidoEntity toEntity(ItemPedido domain) {
        if (domain == null) {
            return null;
        }

        return new ItemPedidoEntity(
            domain.getId().getValue(),
            domain.getProductoId().getValue(),
            domain.getNombreProducto(),
            domain.getCantidad(),
            domain.getPrecioUnitario(),
            domain.getObservacion(),
            domain.getMontoDescuento(),
            domain.getNombrePromocion(),
            domain.getPromocionId()
        );
    }
}
