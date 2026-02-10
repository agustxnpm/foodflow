package com.agustinpalma.comandas.infrastructure.mapper;

import com.agustinpalma.comandas.domain.model.DescuentoManual;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;
import com.agustinpalma.comandas.domain.model.ItemPedido;
import com.agustinpalma.comandas.domain.model.Pago;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.infrastructure.persistence.entity.ItemPedidoEntity;
import com.agustinpalma.comandas.infrastructure.persistence.entity.PagoEntity;
import com.agustinpalma.comandas.infrastructure.persistence.entity.PedidoEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper entre entidades de dominio Pedido y entidades JPA PedidoEntity.
 * Actúa como anti-corruption layer, protegiendo el dominio de detalles de persistencia.
 * 
 * HU-07: Utiliza la relación @OneToMany para persistencia atómica de pedido + ítems.
 * HU-14: Mapea descuento global dinámico (DescuentoManual VO <-> campos DB).
 * Cierre: Mapea pagos y campos de snapshot contable.
 */
@Component
public class PedidoMapper {

    private final ItemPedidoMapper itemPedidoMapper;

    public PedidoMapper(ItemPedidoMapper itemPedidoMapper) {
        this.itemPedidoMapper = itemPedidoMapper;
    }
    
    /**
     * Expone el ItemPedidoMapper para uso en el repositorio al sincronizar entidades.
     */
    public ItemPedidoMapper getItemPedidoMapper() {
        return itemPedidoMapper;
    }

    /**
     * Convierte de entidad JPA a entidad de dominio.
     * 
     * HU-07: Reconstruye el pedido completo con todos sus ítems desde la relación @OneToMany.
     * HU-14: Reconstruye descuento global dinámico desde campos de la BD.
     * Cierre: Reconstruye pagos y snapshot contable.
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
        for (ItemPedidoEntity itemEntity : entity.getItems()) {
            ItemPedido item = itemPedidoMapper.toDomain(itemEntity);
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

        // Reconstruir pagos desde la relación @OneToMany
        for (PagoEntity pagoEntity : entity.getPagos()) {
            Pago pago = new Pago(
                pagoEntity.getMedioPago(),
                pagoEntity.getMonto(),
                pagoEntity.getFecha()
            );
            pedido.agregarPagoDesdePersistencia(pago);
        }

        // Reconstruir snapshot contable
        pedido.setMontoSubtotalFinalDesdePersistencia(entity.getMontoSubtotalFinal());
        pedido.setMontoDescuentosFinalDesdePersistencia(entity.getMontoDescuentosFinal());
        pedido.setMontoTotalFinalDesdePersistencia(entity.getMontoTotalFinal());

        return pedido;
    }

    /**
     * Convierte de entidad de dominio a entidad JPA.
     * 
     * Utiliza la relación bidireccional para garantizar persistencia atómica.
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
        } else {
            entity.setDescGlobalPorcentaje(null);
            entity.setDescGlobalRazon(null);
            entity.setDescGlobalUsuarioId(null);
            entity.setDescGlobalFecha(null);
        }

        // Snapshot contable
        entity.setMontoSubtotalFinal(pedido.getMontoSubtotalFinal());
        entity.setMontoDescuentosFinal(pedido.getMontoDescuentosFinal());
        entity.setMontoTotalFinal(pedido.getMontoTotalFinal());

        // Convertir y agregar ítems
        for (ItemPedido item : pedido.getItems()) {
            ItemPedidoEntity itemEntity = itemPedidoMapper.toEntity(item);
            entity.agregarItem(itemEntity);
        }

        // Convertir y agregar pagos
        for (Pago pago : pedido.getPagos()) {
            PagoEntity pagoEntity = new PagoEntity(
                pago.getMedio(),
                pago.getMonto(),
                pago.getFecha()
            );
            entity.agregarPago(pagoEntity);
        }

        return entity;
    }
}
