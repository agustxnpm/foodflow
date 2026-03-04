-- ============================================================
-- V15__allow_ingreso_tipo_movimiento_caja.sql
-- Amplía el CHECK constraint de movimientos_caja para permitir
-- el tipo INGRESO (registro de cobros de plataformas externas).
-- ============================================================

ALTER TABLE movimientos_caja
    DROP CONSTRAINT chk_movimiento_tipo;

ALTER TABLE movimientos_caja
    ADD CONSTRAINT chk_movimiento_tipo CHECK (tipo IN ('EGRESO', 'INGRESO'));
