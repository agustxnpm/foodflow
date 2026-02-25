package com.agustinpalma.comandas.domain.repository;

import com.agustinpalma.comandas.domain.model.Categoria;
import com.agustinpalma.comandas.domain.model.DomainIds.CategoriaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import java.util.List;
import java.util.Optional;

/**
 * Contrato del repositorio de categorías.
 * Define las operaciones de persistencia sin acoplarse a tecnologías específicas.
 * La implementación concreta reside en la capa de infraestructura.
 */
public interface CategoriaRepository {

    /**
     * Busca una categoría por su identificador.
     *
     * @param id identificador de la categoría
     * @return Optional con la categoría si existe
     */
    Optional<Categoria> buscarPorId(CategoriaId id);

    /**
     * Busca una categoría por su identificador y local.
     * Útil para validar multi-tenancy.
     *
     * @param id identificador de la categoría
     * @param localId identificador del local
     * @return Optional con la categoría si pertenece al local
     */
    Optional<Categoria> buscarPorIdYLocal(CategoriaId id, LocalId localId);

    /**
     * Busca todas las categorías de un local, ordenadas por campo 'orden'.
     *
     * @param localId identificador del local
     * @return lista de categorías ordenadas (puede estar vacía)
     */
    List<Categoria> buscarPorLocal(LocalId localId);

    /**
     * Persiste una categoría nueva o actualiza una existente.
     *
     * @param categoria la categoría a guardar
     * @return la categoría guardada
     */
    Categoria guardar(Categoria categoria);

    /**
     * Verifica si existe una categoría con el nombre especificado en el local.
     * La comparación es case-insensitive.
     *
     * @param nombre nombre de la categoría
     * @param localId identificador del local
     * @return true si existe
     */
    boolean existePorNombreYLocal(String nombre, LocalId localId);

    /**
     * Verifica si existe una categoría con el nombre especificado en el local,
     * excluyendo una categoría específica (útil para validar edición).
     *
     * @param nombre nombre de la categoría
     * @param localId identificador del local
     * @param categoriaIdExcluida ID de la categoría a excluir
     * @return true si existe otra categoría con ese nombre
     */
    boolean existePorNombreYLocalExcluyendo(String nombre, LocalId localId, CategoriaId categoriaIdExcluida);

    /**
     * Elimina una categoría del repositorio.
     *
     * @param id identificador de la categoría a eliminar
     */
    void eliminar(CategoriaId id);
}
