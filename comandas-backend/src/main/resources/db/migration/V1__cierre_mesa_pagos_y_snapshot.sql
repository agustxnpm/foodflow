-- ============================================================
-- V1__cierre_mesa_pagos_y_snapshot.sql
-- Migración Flyway: Soporte para cierre de mesa con pagos
-- parciales y snapshot contable del pedido.
-- ============================================================

-- 1. Tabla de pagos del pedido (soporte split / pagos parciales)
CREATE TABLE IF NOT EXISTS pedidos_pagos (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pedido_id       UUID NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    medio_pago      VARCHAR(20) NOT NULL,
    monto           DECIMAL(10,2) NOT NULL,
    fecha           TIMESTAMP NOT NULL,
    CONSTRAINT chk_pago_monto_positivo CHECK (monto > 0)
);

CREATE INDEX IF NOT EXISTS idx_pago_pedido_id ON pedidos_pagos(pedido_id);

-- 2. Campos de snapshot contable en pedidos
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS monto_subtotal_final DECIMAL(10,2);
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS monto_descuentos_final DECIMAL(10,2);
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS monto_total_final DECIMAL(10,2);
