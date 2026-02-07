package com.agustinpalma.comandas.infrastructure.mapper;

import com.agustinpalma.comandas.domain.model.DescuentoManual;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;
import com.agustinpalma.comandas.domain.model.ItemPedido;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.infrastructure.persistence.entity.ItemPedidoEntity;
import com.agustinpalma.comandas.infrastructure.persistence.entity.PedidoEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper entre entidades de dominio Pedido y entidades JPA PedidoEntity.
 * Actúa como anti-corruption layer, protegiendo el dominio de detalles de persistencia.
 * 
 * HU-07: Utiliza la relación @OneToMany para persistencia atómica de pedido + ítems.
 * HU-14: Mapea descuento global dinámico (DescuentoManual VO <-> campos DB).
 */
@Component
public class PedidoMapper {

    private final ItemPedidoMapper itemPedidoMapper;

    public PedidoMapper(ItemPedidoMapper itemPedidoMapper) {
        this.itemPedidoMapper = itemPedidoMapper;
    }

    /**
     * Convierte de entidad JPA a entidad de dominio.
     * 
     * HU-07: Reconstruye el pedido completo con todos sus ítems desde la relación @OneToMany.
     * HU-14: Reconstruye descuento global dinámico desde campos de la BD.
     * No es necesario consultar la BD por separado - JPA ya carga los ítems.
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

        // HU-07: Cargar ítems desde la relación @OneToMany
        // Los ítems ya están cargados por JPA, no necesitamos consultar por separado
        for (ItemPedidoEntity itemEntity : entity.getItems()) {
            ItemPedido item = itemPedidoMapper.toDomain(itemEntity);
            // Usar método package-private para reconstrucción desde persistencia
            pedido.agregarItemDesdePersistencia(item);
        }

        // HU-14: Reconstruir descuento global si existe en la BD
        if (entity.getDescGlobalPorcentaje() != null) {
            DescuentoManual descuentoGlobal = new DescuentoManual(
                entity.getDescGlobalPorcentaje(),
                entity.getDescGlobalRazon(),
                entity.getDescGlobalUsuarioId(),
                entity.getDescGlobalFecha()
            );
            pedido.aplicarDescuentoGlobal(descuentoGlobal);
        }

        return pedido;
    }

    /**
     * Convierte de entidad de dominio a entidad JPA.
     * 
     * Utiliza la relación bidireccional para garantizar persistencia atómica.
     * Al guardar el PedidoEntity, JPA automáticamente persiste todos los ítems
     * gracias a cascade = CascadeType.ALL.
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

        // HU-14: Descomponer descuento global VO en campos individuales
        if (pedido.getDescuentoGlobal() != null) {
            DescuentoManual dg = pedido.getDescuentoGlobal();
            entity.setDescGlobalPorcentaje(dg.getPorcentaje());
            entity.setDescGlobalRazon(dg.getRazon());
            entity.setDescGlobalUsuarioId(dg.getUsuarioId());
            entity.setDescGlobalFecha(dg.getFechaAplicacion());
        }

        // Convertir y agregar ítems usando la relación bidireccional
        // El método agregarItem() mantiene la sincronización pedido ↔ item
        // Gracias a cascade = CascadeType.ALL, al guardar el pedido se guardan automáticamente los ítems
        for (ItemPedido item : pedido.getItems()) {
            ItemPedidoEntity itemEntity = itemPedidoMapper.toEntity(item);
            entity.agregarItem(itemEntity);
        }

        return entity;
    }
}
