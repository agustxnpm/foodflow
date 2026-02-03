package com.agustinpalma.comandas.infrastructure.persistence.jpa;

import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPedido;
import com.agustinpalma.comandas.infrastructure.persistence.entity.PedidoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para pedidos.
 * Interfaz tecnológica que delega las operaciones CRUD a Spring Data.
 */
@Repository
public interface SpringDataPedidoRepository extends JpaRepository<PedidoEntity, UUID> {

    /**
     * Busca un pedido en un estado específico para una mesa dada.
     *
     * @param mesaId UUID de la mesa
     * @param estado estado del pedido a buscar
     * @return Optional con el pedido si existe
     */
    @Query("SELECT p FROM PedidoEntity p WHERE p.mesaId = :mesaId AND p.estado = :estado")
    Optional<PedidoEntity> findByMesaIdAndEstado(
        @Param("mesaId") UUID mesaId, 
        @Param("estado") EstadoPedido estado
    );

    /**
     * Obtiene el número máximo de pedido para un local específico.
     * Se usa para calcular el siguiente número secuencial.
     *
     * @param localId UUID del local
     * @return el número máximo encontrado, o 0 si no hay pedidos
     */
    @Query("SELECT COALESCE(MAX(p.numero), 0) FROM PedidoEntity p WHERE p.localId = :localId")
    int findMaxNumeroByLocalId(@Param("localId") UUID localId);
}
