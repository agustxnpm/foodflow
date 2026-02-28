-- ============================================================
-- V11: Tabla para registrar el cierre de jornadas operativas de caja.
--
-- Una jornada representa un ciclo operativo cerrado (día de trabajo).
-- La fecha operativa puede diferir de la fecha calendario del cierre
-- para turnos noche (cierre después de medianoche → día anterior).
--
-- Reglas:
-- - Combinación (local_id, fecha_operativa) es única (no doble cierre)
-- - Todos los montos son snapshots al momento del cierre
-- - El balance_efectivo puede ser negativo si los egresos superaron las ventas
-- ============================================================

CREATE TABLE IF NOT EXISTS jornadas_caja (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    local_id                UUID NOT NULL,
    fecha_operativa         DATE NOT NULL,
    fecha_cierre            TIMESTAMP NOT NULL,
    total_ventas_reales     DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_consumo_interno   DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_egresos           DECIMAL(12,2) NOT NULL DEFAULT 0,
    balance_efectivo        DECIMAL(12,2) NOT NULL DEFAULT 0,
    pedidos_cerrados_count  INTEGER NOT NULL DEFAULT 0,
    estado                  VARCHAR(20) NOT NULL DEFAULT 'CERRADA',
    CONSTRAINT uk_jornada_local_fecha UNIQUE (local_id, fecha_operativa),
    CONSTRAINT chk_jornada_estado CHECK (estado IN ('CERRADA')),
    CONSTRAINT chk_jornada_pedidos_positivo CHECK (pedidos_cerrados_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_jornada_local_fecha ON jornadas_caja(local_id, fecha_operativa);
