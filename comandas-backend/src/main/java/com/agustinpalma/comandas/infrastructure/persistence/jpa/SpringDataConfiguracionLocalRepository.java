package com.agustinpalma.comandas.infrastructure.persistence.jpa;

import com.agustinpalma.comandas.infrastructure.persistence.entity.ConfiguracionLocalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataConfiguracionLocalRepository extends JpaRepository<ConfiguracionLocalEntity, UUID> {
}
