package com.agustinpalma.comandas.infrastructure.persistence;

import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.MovimientoCaja;
import com.agustinpalma.comandas.domain.repository.MovimientoCajaRepository;
import com.agustinpalma.comandas.infrastructure.mapper.MovimientoCajaMapper;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataMovimientoCajaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementación JPA del repositorio de movimientos de caja.
 * Adaptador que conecta el contrato del dominio con Spring Data JPA.
 */
@Repository
@Transactional(readOnly = true)
public class MovimientoCajaRepositoryImpl implements MovimientoCajaRepository {

    private final SpringDataMovimientoCajaRepository springDataRepository;
    private final MovimientoCajaMapper mapper;

    public MovimientoCajaRepositoryImpl(SpringDataMovimientoCajaRepository springDataRepository,
                                         MovimientoCajaMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public MovimientoCaja guardar(MovimientoCaja movimiento) {
        var entity = mapper.toEntity(movimiento);
        var guardado = springDataRepository.save(entity);
        return mapper.toDomain(guardado);
    }

    @Override
    public List<MovimientoCaja> buscarPorFecha(LocalId localId, LocalDateTime inicio, LocalDateTime fin) {
        return springDataRepository
            .findByLocalIdAndFechaBetween(localId.getValue(), inicio, fin)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }
}
