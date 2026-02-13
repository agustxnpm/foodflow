package com.agustinpalma.comandas.infrastructure.persistence.jpa;

import com.agustinpalma.comandas.infrastructure.persistence.entity.MovimientoStockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for stock movements.
 * HU-22: Stock management.
 */
@Repository
public interface SpringDataMovimientoStockRepository extends JpaRepository<MovimientoStockEntity, UUID> {

    List<MovimientoStockEntity> findByProductoIdAndLocalIdOrderByFechaDesc(UUID productoId, UUID localId);
}
