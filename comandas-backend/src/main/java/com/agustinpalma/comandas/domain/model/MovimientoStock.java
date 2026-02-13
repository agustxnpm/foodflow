package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainEnums.TipoMovimientoStock;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MovimientoStockId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad que representa un movimiento de stock (auditoría).
 * Cada cambio en el inventario de un producto genera un registro inmutable.
 * 
 * HU-22: Gestión de inventario con auditoría completa.
 * 
 * Reglas de negocio:
 * - La cantidad puede ser positiva (ingreso) o negativa (egreso)
 * - El tipo indica la razón del movimiento
 * - El motivo es una descripción libre para contexto humano
 * - Los movimientos son inmutables una vez creados (no se editan ni borran)
 */
public class MovimientoStock {

    private final MovimientoStockId id;
    private final ProductoId productoId;
    private final LocalId localId;
    private final int cantidad;
    private final TipoMovimientoStock tipo;
    private final LocalDateTime fecha;
    private final String motivo;

    public MovimientoStock(
            MovimientoStockId id,
            ProductoId productoId,
            LocalId localId,
            int cantidad,
            TipoMovimientoStock tipo,
            LocalDateTime fecha,
            String motivo
    ) {
        this.id = Objects.requireNonNull(id, "El id del movimiento no puede ser null");
        this.productoId = Objects.requireNonNull(productoId, "El productoId no puede ser null");
        this.localId = Objects.requireNonNull(localId, "El localId no puede ser null");
        this.tipo = Objects.requireNonNull(tipo, "El tipo de movimiento no puede ser null");
        this.fecha = Objects.requireNonNull(fecha, "La fecha no puede ser null");
        this.motivo = validarMotivo(motivo);

        if (cantidad == 0) {
            throw new IllegalArgumentException("La cantidad del movimiento no puede ser cero");
        }
        this.cantidad = cantidad;
    }

    private String validarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo del movimiento no puede estar vacío");
        }
        return motivo.trim();
    }

    public MovimientoStockId getId() {
        return id;
    }

    public ProductoId getProductoId() {
        return productoId;
    }

    public LocalId getLocalId() {
        return localId;
    }

    public int getCantidad() {
        return cantidad;
    }

    public TipoMovimientoStock getTipo() {
        return tipo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getMotivo() {
        return motivo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MovimientoStock that = (MovimientoStock) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
