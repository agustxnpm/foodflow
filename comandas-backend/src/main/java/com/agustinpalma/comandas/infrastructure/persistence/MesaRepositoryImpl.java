package com.agustinpalma.comandas.infrastructure.persistence;

import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.infrastructure.mapper.MesaMapper;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataMesaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación JPA del repositorio de mesas.
 * Adaptador que conecta el contrato del dominio con Spring Data JPA.
 * Aquí SÍ viven las anotaciones de Spring, aisladas del dominio.
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
}
