package com.agustinpalma.comandas.infrastructure.persistence;

import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.JornadaCaja;
import com.agustinpalma.comandas.domain.repository.JornadaCajaRepository;
import com.agustinpalma.comandas.infrastructure.mapper.JornadaCajaMapper;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataJornadaCajaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Implementación JPA del repositorio de jornadas de caja.
 * Adaptador que conecta el contrato del dominio con Spring Data JPA.
 */
@Repository
@Transactional(readOnly = true)
public class JornadaCajaRepositoryImpl implements JornadaCajaRepository {

    private final SpringDataJornadaCajaRepository springDataRepository;
    private final JornadaCajaMapper mapper;

    public JornadaCajaRepositoryImpl(SpringDataJornadaCajaRepository springDataRepository,
                                      JornadaCajaMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public JornadaCaja guardar(JornadaCaja jornada) {
        var entity = mapper.toEntity(jornada);
        var guardado = springDataRepository.save(entity);
        return mapper.toDomain(guardado);
    }

    @Override
    public boolean existePorFechaOperativa(LocalId localId, LocalDate fechaOperativa) {
        return springDataRepository.existsByLocalIdAndFechaOperativa(
            localId.getValue(), fechaOperativa
        );
    }

    @Override
    public List<JornadaCaja> buscarPorRangoFecha(LocalId localId, LocalDate desde, LocalDate hasta) {
        return springDataRepository
            .findByLocalIdAndFechaOperativaBetweenOrderByFechaOperativaDesc(
                localId.getValue(), desde, hasta
            )
            .stream()
            .map(mapper::toDomain)
            .toList();
    }
}
