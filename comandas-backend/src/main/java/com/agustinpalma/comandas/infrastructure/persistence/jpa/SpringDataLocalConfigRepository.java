package com.agustinpalma.comandas.infrastructure.persistence.jpa;

import com.agustinpalma.comandas.infrastructure.persistence.entity.LocalConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio Spring Data JPA para la configuración key-value del local.
 * Permite recuperar valores de identidad (local-id, nombre-local, cuit)
 * mediante findById(key).
 */
@Repository
public interface SpringDataLocalConfigRepository extends JpaRepository<LocalConfigEntity, String> {
}
