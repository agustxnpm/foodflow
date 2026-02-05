package com.agustinpalma.comandas.domain.repository;

import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import java.util.List;
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

    /**
     * Verifica si existe un producto con el nombre especificado en el local.
     * La comparación es case-insensitive para garantizar unicidad.
     *
     * @param nombre nombre del producto
     * @param localId identificador del local
     * @return true si existe, false en caso contrario
     */
    boolean existePorNombreYLocal(String nombre, LocalId localId);

    /**
     * Verifica si existe un producto con el nombre especificado en el local,
     * excluyendo un producto específico (útil para validar edición).
     *
     * @param nombre nombre del producto
     * @param localId identificador del local
     * @param productoIdExcluido ID del producto a excluir de la búsqueda
     * @return true si existe otro producto con ese nombre, false en caso contrario
     */
    boolean existePorNombreYLocalExcluyendo(String nombre, LocalId localId, ProductoId productoIdExcluido);

    /**
     * Busca todos los productos de un local.
     *
     * @param localId identificador del local
     * @return lista de productos del local (puede estar vacía)
     */
    List<Producto> buscarPorLocal(LocalId localId);

    /**
     * Busca productos de un local filtrados por color hexadecimal.
     * Útil para filtrado visual en la toma de pedidos.
     *
     * @param localId identificador del local
     * @param colorHex código de color en formato hexadecimal (ej: #FF0000)
     * @return lista de productos que coinciden con el color (puede estar vacía)
     */
    List<Producto> buscarPorLocalYColor(LocalId localId, String colorHex);

    /**
     * Elimina un producto del repositorio.
     * 
     * @param id identificador del producto a eliminar
     */
    void eliminar(ProductoId id);
}
