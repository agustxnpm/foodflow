package com.agustinpalma.comandas.infrastructure.persistence;

import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPedido;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import com.agustinpalma.comandas.infrastructure.mapper.PedidoMapper;
import com.agustinpalma.comandas.infrastructure.persistence.entity.ItemPedidoEntity;
import com.agustinpalma.comandas.infrastructure.persistence.entity.PagoEntity;
import com.agustinpalma.comandas.infrastructure.persistence.entity.PedidoEntity;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataPedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;


/**
 * Implementación JPA del repositorio de pedidos.
 * Adaptador que conecta el contrato del dominio con Spring Data JPA.
 * Aquí SÍ viven las anotaciones de Spring, aisladas del dominio.
 */
@Repository
@Transactional(readOnly = true)
public class PedidoRepositoryImpl implements PedidoRepository {

    private static final Logger log = LoggerFactory.getLogger(PedidoRepositoryImpl.class);
    
    private final SpringDataPedidoRepository springDataRepository;
    private final PedidoMapper mapper;

    public PedidoRepositoryImpl(SpringDataPedidoRepository springDataRepository, PedidoMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Pedido guardar(Pedido pedido) {
        log.debug("Guardando pedido ID={}, items={}", pedido.getId().getValue(), pedido.getItems().size());
        
        // Para que orphanRemoval funcione correctamente, debemos actualizar
        // la entidad existente en lugar de crear una nueva
        var entityOpt = springDataRepository.findById(pedido.getId().getValue());
        
        if (entityOpt.isPresent()) {
            log.debug("Actualizando pedido existente ID={}", pedido.getId().getValue());
            // Actualizar entidad existente para preservar el contexto de persistencia de JPA
            var entity = entityOpt.get();
            sincronizarEntity(entity, pedido);
            log.debug("Pedido sincronizado. Items en entity: {}", entity.getItems().size());
            return mapper.toDomain(entity);
        } else {
            log.debug("Creando nuevo pedido ID={}", pedido.getId().getValue());
            // Crear nueva entidad
            var entity = mapper.toEntity(pedido);
            var guardado = springDataRepository.save(entity);
            return mapper.toDomain(guardado);
        }
    }
    
    /**
     * Sincroniza los datos de un pedido de dominio con una entidad JPA existente.
     * Esto es crucial para que orphanRemoval funcione correctamente al eliminar ítems.
     */
    private void sincronizarEntity(PedidoEntity entity, Pedido pedido) {
        log.debug("Sincronizando entity pedidoId={}, items actuales en entity={}, items en dominio={}", 
                  entity.getId(), entity.getItems().size(), pedido.getItems().size());
        
        // Actualizar campos simples
        entity.setEstado(pedido.getEstado());
        entity.setFechaCierre(pedido.getFechaCierre());
        entity.setMedioPago(pedido.getMedioPago());
        
        // Sincronizar snapshot contable
        entity.setMontoSubtotalFinal(pedido.getMontoSubtotalFinal());
        entity.setMontoDescuentosFinal(pedido.getMontoDescuentosFinal());
        entity.setMontoTotalFinal(pedido.getMontoTotalFinal());
        
        // Sincronizar descuento global
        if (pedido.getDescuentoGlobal() != null) {
            var dg = pedido.getDescuentoGlobal();
            log.debug("Aplicando descuento global: {}%", dg.getPorcentaje());
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
        
        // Sincronizar ítems: crear mapa de ítems existentes por ID
        Map<UUID, ItemPedidoEntity> existingItemsMap = new HashMap<>();
        for (ItemPedidoEntity existingItem : entity.getItems()) {
            existingItemsMap.put(existingItem.getId(), existingItem);
            log.debug("Item existente en entity: id={}, producto={}, cantidad={}", 
                      existingItem.getId(), existingItem.getNombreProducto(), existingItem.getCantidad());
        }
        
        // Crear set con IDs de ítems nuevos
        Set<UUID> newItemIds = new HashSet<>();
        for (var domainItem : pedido.getItems()) {
            newItemIds.add(domainItem.getId().getValue());
            log.debug("Item en dominio: id={}, producto={}, cantidad={}", 
                      domainItem.getId().getValue(), domainItem.getNombreProducto(), domainItem.getCantidad());
        }
        
        // Remover ítems que ya no existen (orphanRemoval se activará)
        List<ItemPedidoEntity> itemsToRemove = new ArrayList<>();
        for (ItemPedidoEntity existingItem : entity.getItems()) {
            if (!newItemIds.contains(existingItem.getId())) {
                itemsToRemove.add(existingItem);
                log.debug("Marcando item para eliminar: id={}, producto={}", 
                          existingItem.getId(), existingItem.getNombreProducto());
            }
        }
        for (ItemPedidoEntity itemToRemove : itemsToRemove) {
            log.debug("Eliminando item: id={}", itemToRemove.getId());
            entity.eliminarItem(itemToRemove);
        }
        
        // Actualizar o crear ítems
        for (var domainItem : pedido.getItems()) {
            ItemPedidoEntity existingItem = existingItemsMap.get(domainItem.getId().getValue());
            if (existingItem != null) {
                log.debug("Actualizando item existente: id={}, cantidad {} -> {}", 
                          domainItem.getId().getValue(), existingItem.getCantidad(), domainItem.getCantidad());
                // Actualizar item existente
                existingItem.setCantidad(domainItem.getCantidad());
                existingItem.setObservacion(domainItem.getObservacion());
                existingItem.setMontoDescuento(domainItem.getMontoDescuento());
                existingItem.setNombrePromocion(domainItem.getNombrePromocion());
                existingItem.setPromocionId(domainItem.getPromocionId());
                
                // Actualizar descuento manual si existe
                if (domainItem.getDescuentoManual() != null) {
                    var dm = domainItem.getDescuentoManual();
                    existingItem.setDescManualPorcentaje(dm.getPorcentaje());
                    existingItem.setDescManualRazon(dm.getRazon());
                    existingItem.setDescManualUsuarioId(dm.getUsuarioId());
                    existingItem.setDescManualFecha(dm.getFechaAplicacion());
                } else {
                    existingItem.setDescManualPorcentaje(null);
                    existingItem.setDescManualRazon(null);
                    existingItem.setDescManualUsuarioId(null);
                    existingItem.setDescManualFecha(null);
                }

                // HU-05.1 + HU-22: Sincronizar extras
                existingItem.getExtras().clear();
                for (var extra : domainItem.getExtras()) {
                    existingItem.getExtras().add(new com.agustinpalma.comandas.infrastructure.persistence.entity.ExtraPedidoEmbeddable(
                        extra.getProductoId().getValue(),
                        extra.getNombre(),
                        extra.getPrecioSnapshot()
                    ));
                }
            } else {
                log.debug("Creando nuevo item: id={}, producto={}", 
                          domainItem.getId().getValue(), domainItem.getNombreProducto());
                // Crear nuevo item
                ItemPedidoEntity newItemEntity = mapper.getItemPedidoMapper().toEntity(domainItem);
                entity.agregarItem(newItemEntity);
            }
        }
        
        log.debug("Sincronización completada. Items finales en entity: {}", entity.getItems().size());
        
        // HU-14: Sincronizar pagos (agregar al cerrar, eliminar al reabrir con orphanRemoval)
        // La reapertura limpia pedido.getPagos(), por lo que entity.getPagos().clear() activará orphanRemoval
        entity.getPagos().clear(); // Limpia todos los pagos del entity (orphanRemoval los eliminará físicamente)
        for (var pagoDominio : pedido.getPagos()) {
            var pagoEntity = new PagoEntity(
                pagoDominio.getMedio(),
                pagoDominio.getMonto(),
                pagoDominio.getFecha()
            );
            entity.agregarPago(pagoEntity);
        }
        log.debug("Pagos sincronizados. Pagos en entity: {}", entity.getPagos().size());
    }

    @Override
    public Optional<Pedido> buscarPorId(PedidoId id) {
        return springDataRepository
            .findById(id.getValue())
            .map(mapper::toDomain);
    }

    @Override
    public Optional<Pedido> buscarPorMesaYEstado(MesaId mesaId, EstadoPedido estado) {
        return springDataRepository
            .findByMesaIdAndEstado(mesaId.getValue(), estado)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<Pedido> buscarAbiertoPorMesa(MesaId mesaId, LocalId localId) {
        return springDataRepository
            .findByMesaIdAndLocalIdAndEstado(mesaId.getValue(), localId.getValue(), EstadoPedido.ABIERTO)
            .map(mapper::toDomain);
    }

    @Override
    public int obtenerSiguienteNumero(LocalId localId) {
        int maxNumero = springDataRepository.findMaxNumeroByLocalId(localId.getValue());
        return maxNumero + 1;
    }

    @Override
    public List<Pedido> buscarCerradosPorFecha(LocalId localId, LocalDateTime inicio, LocalDateTime fin) {
        return springDataRepository
            .findCerradosByLocalIdAndFechaCierreBetween(localId.getValue(), inicio, fin)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }
}
