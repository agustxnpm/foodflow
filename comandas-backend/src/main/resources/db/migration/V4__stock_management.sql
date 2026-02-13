-- ============================================================
-- V4__stock_management.sql
-- Migración Flyway: HU-22 Gestión de stock
-- Agrega campos de stock a productos y crea tabla de auditoría.
-- ============================================================

-- Agregar campos de control de stock a productos
ALTER TABLE productos ADD COLUMN IF NOT EXISTS stock_actual INTEGER NOT NULL DEFAULT 0;
ALTER TABLE productos ADD COLUMN IF NOT EXISTS controla_stock BOOLEAN NOT NULL DEFAULT false;

-- Tabla de auditoría de movimientos de stock
CREATE TABLE IF NOT EXISTS movimientos_stock (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    producto_id     UUID NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    local_id        UUID NOT NULL,
    cantidad        INTEGER NOT NULL,
    tipo            VARCHAR(30) NOT NULL,
    fecha           TIMESTAMP NOT NULL,
    motivo          VARCHAR(500) NOT NULL,
    CONSTRAINT chk_movimiento_stock_cantidad_no_cero CHECK (cantidad <> 0),
    CONSTRAINT chk_movimiento_stock_tipo CHECK (tipo IN ('VENTA', 'REAPERTURA_PEDIDO', 'AJUSTE_MANUAL', 'INGRESO_MERCADERIA'))
);

CREATE INDEX IF NOT EXISTS idx_movimiento_stock_producto ON movimientos_stock(producto_id, local_id);
CREATE INDEX IF NOT EXISTS idx_movimiento_stock_fecha ON movimientos_stock(local_id, fecha);
