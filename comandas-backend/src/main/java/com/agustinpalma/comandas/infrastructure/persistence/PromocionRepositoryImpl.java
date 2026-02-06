package com.agustinpalma.comandas.infrastructure.persistence;

import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.PromocionId;
import com.agustinpalma.comandas.domain.model.Promocion;
import com.agustinpalma.comandas.domain.repository.PromocionRepository;
import com.agustinpalma.comandas.infrastructure.mapper.PromocionMapper;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataPromocionRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class PromocionRepositoryImpl implements PromocionRepository {

    private final SpringDataPromocionRepository springDataRepository;
    private final PromocionMapper mapper;

    public PromocionRepositoryImpl(SpringDataPromocionRepository springDataRepository, PromocionMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Promocion guardar(Promocion promocion) {
        var entity = mapper.toEntity(promocion);
        var guardada = springDataRepository.save(entity);
        return mapper.toDomain(guardada);
    }

    @Override
    public Optional<Promocion> buscarPorId(PromocionId id) {
        return springDataRepository
                .findById(id.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Promocion> buscarPorIdYLocal(PromocionId id, LocalId localId) {
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
    public List<Promocion> buscarPorLocal(LocalId localId) {
        return springDataRepository.findByLocalId(localId.getValue())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
