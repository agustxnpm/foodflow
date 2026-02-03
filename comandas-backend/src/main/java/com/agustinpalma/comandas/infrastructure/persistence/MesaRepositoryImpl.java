package com.agustinpalma.comandas.infrastructure.persistence;

import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.infrastructure.mapper.MesaMapper;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataMesaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación JPA del repositorio de mesas.
 * Adaptador que conecta el contrato del dominio con Spring Data JPA.
 */
@Repository
@Transactional(readOnly = true)
public class MesaRepositoryImpl implements MesaRepository {

    private final SpringDataMesaRepository springDataRepository;
    private final MesaMapper mapper;

    public MesaRepositoryImpl(SpringDataMesaRepository springDataRepository, MesaMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public List<Mesa> buscarPorLocal(LocalId localId) {
        return springDataRepository
            .findByLocalIdOrderByNumeroAsc(localId.getValue())
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<Mesa> buscarPorId(MesaId mesaId) {
        return springDataRepository
            .findById(mesaId.getValue())
            .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Mesa guardar(Mesa mesa) {
        var entity = mapper.toEntity(mesa);
        var guardada = springDataRepository.save(entity);
        return mapper.toDomain(guardada);
    }

    @Override
    public boolean existePorNumeroYLocal(int numero, LocalId localId) {
        return springDataRepository.existsByNumeroAndLocalId(numero, localId.getValue());
    }

    @Override
    public int contarPorLocal(LocalId localId) {
        return (int) springDataRepository.countByLocalId(localId.getValue());
    }

    @Override
    @Transactional
    public void eliminar(MesaId mesaId) {
        springDataRepository.deleteById(mesaId.getValue());
    }
}
