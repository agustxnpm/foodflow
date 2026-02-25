package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.CategoriaRequest;
import com.agustinpalma.comandas.application.dto.CategoriaResponse;
import com.agustinpalma.comandas.domain.model.Categoria;
import com.agustinpalma.comandas.domain.model.DomainIds.CategoriaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.repository.CategoriaRepository;
import java.util.Objects;

/**
 * Caso de uso para crear una nueva categoría en el catálogo del local.
 *
 * Reglas de negocio:
 * - El nombre debe ser único dentro del local (case insensitive)
 * - El nombre no puede estar vacío
 * - El color se normaliza automáticamente a mayúsculas
 * - La categoría se vincula de forma inmutable al LocalId
 */
public class CrearCategoriaUseCase {

    private final CategoriaRepository categoriaRepository;

    public CrearCategoriaUseCase(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = Objects.requireNonNull(categoriaRepository, "El categoriaRepository es obligatorio");
    }

    /**
     * Ejecuta la creación de una nueva categoría.
     *
     * @param localId identificador del local (tenant)
     * @param request datos de la categoría a crear
     * @return DTO con la información de la categoría creada
     * @throws IllegalArgumentException si el nombre ya existe en el local
     */
    public CategoriaResponse ejecutar(LocalId localId, CategoriaRequest request) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(request, "El request es obligatorio");

        // Validación de negocio: unicidad del nombre dentro del local
        if (categoriaRepository.existePorNombreYLocal(request.nombre(), localId)) {
            throw new IllegalArgumentException(
                "Ya existe una categoría con el nombre '" + request.nombre() + "' en este local"
            );
        }

        CategoriaId nuevoId = CategoriaId.generate();
        boolean admiteVariantes = request.admiteVariantes() != null ? request.admiteVariantes() : false;
        boolean esCategoriaExtra = request.esCategoriaExtra() != null ? request.esCategoriaExtra() : false;
        int orden = request.orden() != null ? request.orden() : 0;

        Categoria nuevaCategoria = new Categoria(
            nuevoId,
            localId,
            request.nombre(),
            request.colorHex(),
            admiteVariantes,
            esCategoriaExtra,
            orden
        );

        Categoria categoriaGuardada = categoriaRepository.guardar(nuevaCategoria);
        return CategoriaResponse.fromDomain(categoriaGuardada);
    }
}
