package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.domain.model.Categoria;
import com.agustinpalma.comandas.domain.model.DomainIds.CategoriaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.repository.CategoriaRepository;
import java.util.Objects;

/**
 * Caso de uso para eliminar una categoría del catálogo.
 *
 * Reglas de negocio:
 * - La categoría debe existir y pertenecer al local
 * - La eliminación es definitiva (no soft-delete)
 *
 * NOTA: Los productos que tenían esta categoría quedarán con categoriaId = null.
 * La FK en la base de datos NO tiene ON DELETE CASCADE, por lo que si existen
 * productos referenciando esta categoría, la eliminación fallará por constraint.
 * Esto es intencional: el usuario debe reclasificar productos antes de eliminar.
 */
public class EliminarCategoriaUseCase {

    private final CategoriaRepository categoriaRepository;

    public EliminarCategoriaUseCase(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = Objects.requireNonNull(categoriaRepository, "El categoriaRepository es obligatorio");
    }

    /**
     * Ejecuta la eliminación de una categoría.
     *
     * @param categoriaId identificador de la categoría a eliminar
     * @param localId identificador del local (validación multi-tenancy)
     * @throws IllegalArgumentException si la categoría no existe o no pertenece al local
     */
    public void ejecutar(CategoriaId categoriaId, LocalId localId) {
        Objects.requireNonNull(categoriaId, "El categoriaId es obligatorio");
        Objects.requireNonNull(localId, "El localId es obligatorio");

        Categoria categoria = categoriaRepository.buscarPorId(categoriaId)
            .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        if (!categoria.getLocalId().equals(localId)) {
            throw new IllegalArgumentException("No tiene permisos para eliminar esta categoría");
        }

        categoriaRepository.eliminar(categoriaId);
    }
}
