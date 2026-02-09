package com.agustinpalma.comandas.infrastructure.persistence.jpa;

import com.agustinpalma.comandas.infrastructure.persistence.entity.ProductoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    /**
     * Verifica si existe un producto con el nombre dado en el local especificado.
     * La comparación es case-insensitive para garantizar unicidad.
     *
     * @param localId UUID del local
     * @param nombre nombre del producto
     * @return true si existe, false en caso contrario
     */
    boolean existsByLocalIdAndNombreIgnoreCase(UUID localId, String nombre);

    /**
     * Verifica si existe un producto con el nombre dado en el local,
     * excluyendo un producto específico.
     *
     * @param localId UUID del local
     * @param nombre nombre del producto
     * @param productoId UUID del producto a excluir
     * @return true si existe otro producto con ese nombre, false en caso contrario
     */
    boolean existsByLocalIdAndNombreIgnoreCaseAndIdNot(UUID localId, String nombre, UUID productoId);

    /**
     * Busca todos los productos de un local.
     *
     * @param localId UUID del local
     * @return lista de productos (puede estar vacía)
     */
    List<ProductoEntity> findByLocalId(UUID localId);

    /**
     * Busca productos de un local filtrados por color hexadecimal.
     *
     * @param localId UUID del local
     * @param colorHex código hexadecimal del color
     * @return lista de productos que coinciden (puede estar vacía)
     */
    List<ProductoEntity> findByLocalIdAndColorHex(UUID localId, String colorHex);

    /**
     * HU-05.1: Busca variantes hermanas del mismo grupo.
     *
     * @param localId UUID del local
     * @param grupoVarianteId UUID del grupo de variantes
     * @return lista de productos variantes (puede estar vacía)
     */
    List<ProductoEntity> findByLocalIdAndGrupoVarianteId(UUID localId, UUID grupoVarianteId);

    /**
     * HU-22: Busca producto extra por nombre y marca de extra.
     *
     * @param localId UUID del local
     * @param nombre nombre del producto (case-insensitive)
     * @param esExtra true para buscar solo extras
     * @return Optional con el producto si existe
     */
    Optional<ProductoEntity> findByLocalIdAndNombreIgnoreCaseAndEsExtra(UUID localId, String nombre, boolean esExtra);
}
