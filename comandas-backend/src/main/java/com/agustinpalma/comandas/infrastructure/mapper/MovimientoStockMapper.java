package com.agustinpalma.comandas.infrastructure.mapper;

import com.agustinpalma.comandas.domain.model.DomainEnums.TipoMovimientoStock;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MovimientoStockId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import com.agustinpalma.comandas.domain.model.MovimientoStock;
import com.agustinpalma.comandas.infrastructure.persistence.entity.MovimientoStockEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper between MovimientoStock domain entity and MovimientoStockEntity JPA entity.
 * HU-22: Stock management.
 */
@Component
public class MovimientoStockMapper {

    public MovimientoStock toDomain(MovimientoStockEntity entity) {
        return new MovimientoStock(
            new MovimientoStockId(entity.getId()),
            new ProductoId(entity.getProductoId()),
            new LocalId(entity.getLocalId()),
            entity.getCantidad(),
            TipoMovimientoStock.valueOf(entity.getTipo()),
            entity.getFecha(),
            entity.getMotivo()
        );
    }

    public MovimientoStockEntity toEntity(MovimientoStock domain) {
        return new MovimientoStockEntity(
            domain.getId().getValue(),
            domain.getProductoId().getValue(),
            domain.getLocalId().getValue(),
            domain.getCantidad(),
            domain.getTipo().name(),
            domain.getFecha(),
            domain.getMotivo()
        );
    }
}
