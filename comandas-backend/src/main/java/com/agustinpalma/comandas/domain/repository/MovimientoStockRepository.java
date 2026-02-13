package com.agustinpalma.comandas.domain.repository;

import com.agustinpalma.comandas.domain.model.MovimientoStock;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;

import java.util.List;

/**
 * Contrato del repositorio de movimientos de stock.
 * Define las operaciones de persistencia sin acoplarse a tecnologías específicas.
 * 
 * HU-22: Gestión de inventario.
 */
public interface MovimientoStockRepository {

    MovimientoStock guardar(MovimientoStock movimiento);

    List<MovimientoStock> buscarPorProducto(ProductoId productoId, LocalId localId);
}
