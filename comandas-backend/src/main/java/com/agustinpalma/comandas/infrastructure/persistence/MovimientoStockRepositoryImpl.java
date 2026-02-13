package com.agustinpalma.comandas.infrastructure.persistence;

import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import com.agustinpalma.comandas.domain.model.MovimientoStock;
import com.agustinpalma.comandas.domain.repository.MovimientoStockRepository;
import com.agustinpalma.comandas.infrastructure.mapper.MovimientoStockMapper;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataMovimientoStockRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * JPA implementation of MovimientoStockRepository.
 * HU-22: Stock management.
 */
@Repository
@Transactional(readOnly = true)
public class MovimientoStockRepositoryImpl implements MovimientoStockRepository {

    private final SpringDataMovimientoStockRepository springDataRepository;
    private final MovimientoStockMapper mapper;

    public MovimientoStockRepositoryImpl(
            SpringDataMovimientoStockRepository springDataRepository,
            MovimientoStockMapper mapper
    ) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public MovimientoStock guardar(MovimientoStock movimiento) {
        var entity = mapper.toEntity(movimiento);
        var guardado = springDataRepository.save(entity);
        return mapper.toDomain(guardado);
    }

    @Override
    public List<MovimientoStock> buscarPorProducto(ProductoId productoId, LocalId localId) {
        return springDataRepository
            .findByProductoIdAndLocalIdOrderByFechaDesc(productoId.getValue(), localId.getValue())
            .stream()
            .map(mapper::toDomain)
            .toList();
    }
}
