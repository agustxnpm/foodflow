package com.agustinpalma.comandas.infrastructure.persistence.jpa;

import com.agustinpalma.comandas.infrastructure.persistence.entity.ProductoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para productos.
 * Interfaz tecnológica que delega las operaciones CRUD a Spring Data.
 */
@Repository
public interface SpringDataProductoRepository extends JpaRepository<ProductoEntity, UUID> {

    /**
     * Busca un producto por su identificador.
     *
     * @param id UUID del producto
     * @return Optional con el producto si existe
     */
    Optional<ProductoEntity> findById(UUID id);
}
