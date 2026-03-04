-- ============================================================
-- V16: Soporte para apertura de caja (Issue 3).
--
-- Cambios:
-- 1. Se permite estado ABIERTA en jornadas_caja (antes solo CERRADA)
-- 2. Se agrega fondo_inicial (BigDecimal) — dinero base declarado al abrir
-- 3. Se agrega fecha_apertura (timestamp de cuando se abrió la caja)
-- 4. fecha_cierre pasa a ser nullable (ABIERTA aún no tiene cierre)
--
-- Datos históricos: jornadas CERRADAS existentes reciben fondo_inicial=0
-- y fecha_apertura=NULL (no existía el concepto al crearse).
-- ============================================================

-- 1. Permitir estado ABIERTA
ALTER TABLE jornadas_caja DROP CONSTRAINT chk_jornada_estado;
ALTER TABLE jornadas_caja ADD CONSTRAINT chk_jornada_estado CHECK (estado IN ('ABIERTA', 'CERRADA'));

-- 2. Fondo inicial del día
ALTER TABLE jornadas_caja ADD COLUMN fondo_inicial DECIMAL(12,2) NOT NULL DEFAULT 0;

-- 3. Timestamp de apertura (nullable para históricos)
ALTER TABLE jornadas_caja ADD COLUMN fecha_apertura TIMESTAMP;

-- 4. fecha_cierre ahora es nullable (ABIERTA no tiene cierre aún)
ALTER TABLE jornadas_caja ALTER COLUMN fecha_cierre DROP NOT NULL;
