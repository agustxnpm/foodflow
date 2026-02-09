package com.agustinpalma.comandas.infrastructure.persistence;

import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import com.agustinpalma.comandas.infrastructure.mapper.ProductoMapper;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataProductoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    @Override
    public boolean existePorNombreYLocal(String nombre, LocalId localId) {
        return springDataRepository.existsByLocalIdAndNombreIgnoreCase(localId.getValue(), nombre);
    }

    @Override
    public boolean existePorNombreYLocalExcluyendo(String nombre, LocalId localId, ProductoId productoIdExcluido) {
        return springDataRepository.existsByLocalIdAndNombreIgnoreCaseAndIdNot(
            localId.getValue(), 
            nombre, 
            productoIdExcluido.getValue()
        );
    }

    @Override
    public List<Producto> buscarPorLocal(LocalId localId) {
        return springDataRepository.findByLocalId(localId.getValue())
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public List<Producto> buscarPorLocalYColor(LocalId localId, String colorHex) {
        return springDataRepository.findByLocalIdAndColorHex(localId.getValue(), colorHex)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    @Transactional
    public void eliminar(ProductoId id) {
        springDataRepository.deleteById(id.getValue());
    }
    
    /**
     * HU-05.1 + HU-22: Busca variantes hermanas del mismo grupo.
     */
    @Override
    public List<Producto> buscarPorGrupoVariante(LocalId localId, ProductoId grupoVarianteId) {
        return springDataRepository.findByLocalIdAndGrupoVarianteId(
            localId.getValue(), 
            grupoVarianteId.getValue()
        ).stream()
        .map(mapper::toDomain)
        .toList();
    }
    
    /**
     * HU-22: Busca el producto extra "disco de carne".
     */
    @Override
    public Optional<Producto> buscarExtraDiscoDeCarne(LocalId localId) {
        return springDataRepository.findByLocalIdAndNombreIgnoreCaseAndEsExtra(
            localId.getValue(),
            "Disco de Carne",
            true
        ).map(mapper::toDomain);
    }
}
