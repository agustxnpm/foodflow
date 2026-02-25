package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.CategoriaResponse;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.repository.CategoriaRepository;
import java.util.List;
import java.util.Objects;

/**
 * Caso de uso para consultar las categorías de un local.
 * Retorna las categorías ordenadas por campo 'orden' (ascendente).
 */
public class ConsultarCategoriasUseCase {

    private final CategoriaRepository categoriaRepository;

    public ConsultarCategoriasUseCase(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = Objects.requireNonNull(categoriaRepository, "El categoriaRepository es obligatorio");
    }

    /**
     * Consulta todas las categorías del local, ordenadas por 'orden'.
     *
     * @param localId identificador del local
     * @return lista de categorías (puede estar vacía)
     */
    public List<CategoriaResponse> ejecutar(LocalId localId) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        return categoriaRepository.buscarPorLocal(localId).stream()
            .map(CategoriaResponse::fromDomain)
            .toList();
    }
}
