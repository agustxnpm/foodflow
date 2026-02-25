package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.CategoriaRequest;
import com.agustinpalma.comandas.application.dto.CategoriaResponse;
import com.agustinpalma.comandas.domain.model.Categoria;
import com.agustinpalma.comandas.domain.model.DomainIds.CategoriaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.repository.CategoriaRepository;
import java.util.Objects;

/**
 * Caso de uso para editar una categoría existente del catálogo.
 *
 * Reglas de negocio:
 * - La categoría debe existir y pertenecer al local
 * - El nombre debe seguir siendo único dentro del local (excluyendo la categoría actual)
 */
public class EditarCategoriaUseCase {

    private final CategoriaRepository categoriaRepository;

    public EditarCategoriaUseCase(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = Objects.requireNonNull(categoriaRepository, "El categoriaRepository es obligatorio");
    }

    /**
     * Ejecuta la edición de una categoría existente.
     *
     * @param categoriaId identificador de la categoría a editar
     * @param localId identificador del local (validación multi-tenancy)
     * @param request nuevos datos de la categoría
     * @return DTO con la información actualizada
     * @throws IllegalArgumentException si la categoría no existe o no pertenece al local
     */
    public CategoriaResponse ejecutar(CategoriaId categoriaId, LocalId localId, CategoriaRequest request) {
        Objects.requireNonNull(categoriaId, "El categoriaId es obligatorio");
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(request, "El request es obligatorio");

        // Buscar la categoría existente
        Categoria categoria = categoriaRepository.buscarPorId(categoriaId)
            .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        // Validación multi-tenancy
        if (!categoria.getLocalId().equals(localId)) {
            throw new IllegalArgumentException("No tiene permisos para editar esta categoría");
        }

        // Si cambió el nombre, validar unicidad
        if (!categoria.getNombre().equalsIgnoreCase(request.nombre())) {
            if (categoriaRepository.existePorNombreYLocalExcluyendo(request.nombre(), localId, categoriaId)) {
                throw new IllegalArgumentException(
                    "Ya existe otra categoría con el nombre '" + request.nombre() + "' en este local"
                );
            }
        }

        // Actualizar campos usando métodos de dominio
        categoria.actualizarNombre(request.nombre());
        categoria.actualizarColor(request.colorHex());

        if (request.admiteVariantes() != null) {
            categoria.cambiarAdmiteVariantes(request.admiteVariantes());
        }
        if (request.esCategoriaExtra() != null) {
            categoria.cambiarEsCategoriaExtra(request.esCategoriaExtra());
        }
        if (request.orden() != null) {
            categoria.cambiarOrden(request.orden());
        }

        Categoria categoriaActualizada = categoriaRepository.guardar(categoria);
        return CategoriaResponse.fromDomain(categoriaActualizada);
    }
}
