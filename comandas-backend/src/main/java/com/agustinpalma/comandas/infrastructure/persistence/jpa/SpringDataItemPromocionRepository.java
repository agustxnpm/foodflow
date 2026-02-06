package com.agustinpalma.comandas.infrastructure.persistence.jpa;

import com.agustinpalma.comandas.infrastructure.persistence.entity.ItemPromocionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para items del alcance de promociones.
 * HU-09: Asociar productos a promociones.
 */
@Repository
public interface SpringDataItemPromocionRepository extends JpaRepository<ItemPromocionEntity, UUID> {

    /**
     * Busca todos los items de alcance de una promoción específica.
     */
    List<ItemPromocionEntity> findByPromocionId(UUID promocionId);

    /**
     * Elimina todos los items de alcance de una promoción específica.
     * Útil para reemplazar el alcance completo.
     */
    void deleteByPromocionId(UUID promocionId);
}
