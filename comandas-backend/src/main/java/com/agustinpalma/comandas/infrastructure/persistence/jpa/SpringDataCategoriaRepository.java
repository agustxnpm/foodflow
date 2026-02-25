package com.agustinpalma.comandas.infrastructure.persistence.jpa;

import com.agustinpalma.comandas.infrastructure.persistence.entity.CategoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para categorías.
 * Spring genera automáticamente las implementaciones de las queries.
 */
@Repository
public interface SpringDataCategoriaRepository extends JpaRepository<CategoriaEntity, UUID> {

    List<CategoriaEntity> findByLocalIdOrderByOrdenAsc(UUID localId);

    boolean existsByLocalIdAndNombreIgnoreCase(UUID localId, String nombre);

    boolean existsByLocalIdAndNombreIgnoreCaseAndIdNot(UUID localId, String nombre, UUID categoriaId);

    Optional<CategoriaEntity> findByIdAndLocalId(UUID id, UUID localId);
}
