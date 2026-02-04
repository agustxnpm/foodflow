package com.agustinpalma.comandas.domain.repository;

import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import java.util.Optional;

/**
 * Contrato del repositorio de productos.
 * Define las operaciones de persistencia sin acoplarse a tecnologías específicas.
 * La implementación concreta reside en la capa de infraestructura.
 */
public interface ProductoRepository {

    /**
     * Busca un producto por su identificador.
     *
     * @param id identificador del producto
     * @return Optional con el producto si existe, vacío en caso contrario
     */
    Optional<Producto> buscarPorId(ProductoId id);

    /**
     * Busca un producto por su identificador y local.
     * Útil para validar multi-tenancy.
     *
     * @param id identificador del producto
     * @param localId identificador del local
     * @return Optional con el producto si existe y pertenece al local, vacío en caso contrario
     */
    Optional<Producto> buscarPorIdYLocal(ProductoId id, LocalId localId);

    /**
     * Persiste un producto nuevo o actualiza uno existente.
     *
     * @param producto el producto a guardar
     * @return el producto guardado
     */
    Producto guardar(Producto producto);
}
