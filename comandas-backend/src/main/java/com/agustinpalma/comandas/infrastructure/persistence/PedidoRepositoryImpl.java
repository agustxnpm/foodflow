package com.agustinpalma.comandas.infrastructure.persistence;

import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPedido;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import com.agustinpalma.comandas.infrastructure.mapper.PedidoMapper;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataPedidoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementación JPA del repositorio de pedidos.
 * Adaptador que conecta el contrato del dominio con Spring Data JPA.
 * Aquí SÍ viven las anotaciones de Spring, aisladas del dominio.
 */
@Repository
@Transactional(readOnly = true)
public class PedidoRepositoryImpl implements PedidoRepository {

    private final SpringDataPedidoRepository springDataRepository;
    private final PedidoMapper mapper;

    public PedidoRepositoryImpl(SpringDataPedidoRepository springDataRepository, PedidoMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Pedido guardar(Pedido pedido) {
        var entity = mapper.toEntity(pedido);
        var guardado = springDataRepository.save(entity);
        return mapper.toDomain(guardado);
    }

    @Override
    public Optional<Pedido> buscarPorId(PedidoId id) {
        return springDataRepository
            .findById(id.getValue())
            .map(mapper::toDomain);
    }

    @Override
    public Optional<Pedido> buscarPorMesaYEstado(MesaId mesaId, EstadoPedido estado) {
        return springDataRepository
            .findByMesaIdAndEstado(mesaId.getValue(), estado)
            .map(mapper::toDomain);
    }

    @Override
    public int obtenerSiguienteNumero(LocalId localId) {
        int maxNumero = springDataRepository.findMaxNumeroByLocalId(localId.getValue());
        return maxNumero + 1;
    }
}
