package com.agustinpalma.comandas.infrastructure.persistence.jpa;

import com.agustinpalma.comandas.infrastructure.persistence.entity.PromocionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataPromocionRepository extends JpaRepository<PromocionEntity, UUID> {

    boolean existsByLocalIdAndNombreIgnoreCase(UUID localId, String nombre);

    List<PromocionEntity> findByLocalId(UUID localId);
}
