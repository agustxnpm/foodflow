package com.agustinpalma.comandas.infrastructure.persistence.jpa;

import com.agustinpalma.comandas.infrastructure.persistence.entity.LocalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repositorio Spring Data JPA para locales.
 * Usado exclusivamente por el seed de inicialización.
 */
@Repository
public interface SpringDataLocalRepository extends JpaRepository<LocalEntity, UUID> {
}
