package com.agustinpalma.comandas.infrastructure.persistence.jpa;

import com.agustinpalma.comandas.infrastructure.persistence.entity.ItemPedidoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para items de pedido.
 * Interfaz tecnológica que delega las operaciones CRUD a Spring Data.
 */
@Repository
public interface SpringDataItemPedidoRepository extends JpaRepository<ItemPedidoEntity, UUID> {

    /**
     * Busca todos los items de un pedido específico.
     *
     * @param pedidoId UUID del pedido
     * @return lista de items del pedido
     */
    @Query("SELECT i FROM ItemPedidoEntity i WHERE i.pedidoId = :pedidoId")
    List<ItemPedidoEntity> findByPedidoId(@Param("pedidoId") UUID pedidoId);
}
