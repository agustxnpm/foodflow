package com.agustinpalma.comandas.infrastructure.persistence.jpa;

import com.agustinpalma.comandas.infrastructure.persistence.entity.PagoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repositorio Spring Data JPA para pagos.
 * 
 * Este repositorio se usa principalmente en tests de integración
 * para verificar la eliminación física de pagos (orphanRemoval).
 */
@Repository
public interface SpringDataPagoRepository extends JpaRepository<PagoEntity, UUID> {
}
