package com.agustinpalma.comandas.infrastructure.mapper;

import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;
import com.agustinpalma.comandas.domain.model.ItemPedido;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.infrastructure.persistence.entity.ItemPedidoEntity;
import com.agustinpalma.comandas.infrastructure.persistence.entity.PedidoEntity;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataItemPedidoRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper entre entidades de dominio Pedido y entidades JPA PedidoEntity.
 * Actúa como anti-corruption layer, protegiendo el dominio de detalles de persistencia.
 */
@Component
public class PedidoMapper {

    private final ItemPedidoMapper itemPedidoMapper;
    private final SpringDataItemPedidoRepository itemPedidoRepository;

    public PedidoMapper(ItemPedidoMapper itemPedidoMapper, SpringDataItemPedidoRepository itemPedidoRepository) {
        this.itemPedidoMapper = itemPedidoMapper;
        this.itemPedidoRepository = itemPedidoRepository;
    }

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
        Pedido pedido = new Pedido(
            new PedidoId(entity.getId()),
            new LocalId(entity.getLocalId()),
            new MesaId(entity.getMesaId()),
            entity.getNumero(),
            entity.getEstado(),
            entity.getFechaApertura()
        );

        // Cargar items del pedido desde la base de datos
        List<ItemPedidoEntity> itemsEntities = itemPedidoRepository.findByPedidoId(entity.getId());
        for (ItemPedidoEntity itemEntity : itemsEntities) {
            ItemPedido item = itemPedidoMapper.toDomain(itemEntity);
            // Usar método package-private para reconstrucción desde persistencia
            pedido.agregarItemDesdePersistencia(item);
        }

        return pedido;
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

        // Persistir items del pedido
        // IMPORTANTE: Los items se persisten por separado en el repositorio
        // Aquí solo convertimos la entidad principal
        for (ItemPedido item : pedido.getItems()) {
            ItemPedidoEntity itemEntity = itemPedidoMapper.toEntity(item);
            itemPedidoRepository.save(itemEntity);
        }

        return entity;
    }
}
