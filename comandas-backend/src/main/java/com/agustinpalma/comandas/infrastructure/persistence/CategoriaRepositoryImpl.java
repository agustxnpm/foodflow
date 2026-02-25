package com.agustinpalma.comandas.infrastructure.persistence;

import com.agustinpalma.comandas.domain.model.Categoria;
import com.agustinpalma.comandas.domain.model.DomainIds.CategoriaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.repository.CategoriaRepository;
import com.agustinpalma.comandas.infrastructure.mapper.CategoriaMapper;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataCategoriaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementación JPA del repositorio de categorías.
 * Adapta el contrato de dominio a Spring Data JPA.
 */
@Repository
@Transactional(readOnly = true)
public class CategoriaRepositoryImpl implements CategoriaRepository {

    private final SpringDataCategoriaRepository springDataRepository;
    private final CategoriaMapper mapper;

    public CategoriaRepositoryImpl(SpringDataCategoriaRepository springDataRepository, CategoriaMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Categoria> buscarPorId(CategoriaId id) {
        return springDataRepository.findById(id.getValue()).map(mapper::toDomain);
    }

    @Override
    public Optional<Categoria> buscarPorIdYLocal(CategoriaId id, LocalId localId) {
        return springDataRepository.findByIdAndLocalId(id.getValue(), localId.getValue())
            .map(mapper::toDomain);
    }

    @Override
    public List<Categoria> buscarPorLocal(LocalId localId) {
        return springDataRepository.findByLocalIdOrderByOrdenAsc(localId.getValue()).stream()
            .map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public Categoria guardar(Categoria categoria) {
        var entity = mapper.toEntity(categoria);
        var guardado = springDataRepository.save(entity);
        return mapper.toDomain(guardado);
    }

    @Override
    public boolean existePorNombreYLocal(String nombre, LocalId localId) {
        return springDataRepository.existsByLocalIdAndNombreIgnoreCase(localId.getValue(), nombre);
    }

    @Override
    public boolean existePorNombreYLocalExcluyendo(String nombre, LocalId localId, CategoriaId categoriaIdExcluida) {
        return springDataRepository.existsByLocalIdAndNombreIgnoreCaseAndIdNot(
            localId.getValue(), nombre, categoriaIdExcluida.getValue());
    }

    @Override
    @Transactional
    public void eliminar(CategoriaId id) {
        springDataRepository.deleteById(id.getValue());
    }
}
