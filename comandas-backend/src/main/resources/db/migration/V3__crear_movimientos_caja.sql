-- ============================================================
-- V3__crear_movimientos_caja.sql
-- Migración Flyway: Tabla de movimientos de caja (egresos)
-- para soporte del módulo de Control de Caja.
-- ============================================================

-- 1. Tabla de movimientos de caja (egresos de efectivo)
CREATE TABLE IF NOT EXISTS movimientos_caja (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    local_id             UUID NOT NULL,
    monto                DECIMAL(10,2) NOT NULL,
    descripcion          VARCHAR(500) NOT NULL,
    fecha                TIMESTAMP NOT NULL,
    tipo                 VARCHAR(20) NOT NULL,
    numero_comprobante   VARCHAR(50) NOT NULL UNIQUE,
    CONSTRAINT chk_movimiento_monto_positivo CHECK (monto > 0),
    CONSTRAINT chk_movimiento_tipo CHECK (tipo IN ('EGRESO'))
);

CREATE INDEX IF NOT EXISTS idx_movimiento_caja_local_fecha ON movimientos_caja(local_id, fecha);
