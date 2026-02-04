package com.agustinpalma.comandas.infrastructure.persistence;

import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import com.agustinpalma.comandas.infrastructure.mapper.ProductoMapper;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataProductoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementación JPA del repositorio de productos.
 * Adaptador que conecta el contrato del dominio con Spring Data JPA.
 * Aquí SÍ viven las anotaciones de Spring, aisladas del dominio.
 */
@Repository
@Transactional(readOnly = true)
public class ProductoRepositoryImpl implements ProductoRepository {

    private final SpringDataProductoRepository springDataRepository;
    private final ProductoMapper mapper;

    public ProductoRepositoryImpl(SpringDataProductoRepository springDataRepository, ProductoMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Producto guardar(Producto producto) {
        var entity = mapper.toEntity(producto);
        var guardado = springDataRepository.save(entity);
        return mapper.toDomain(guardado);
    }

    @Override
    public Optional<Producto> buscarPorId(ProductoId id) {
        return springDataRepository
            .findById(id.getValue())
            .map(mapper::toDomain);
    }

    @Override
    public Optional<Producto> buscarPorIdYLocal(ProductoId id, LocalId localId) {
        return springDataRepository
            .findById(id.getValue())
            .filter(entity -> entity.getLocalId().equals(localId.getValue()))
            .map(mapper::toDomain);
    }
}
