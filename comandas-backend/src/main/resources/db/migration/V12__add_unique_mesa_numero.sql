-- ============================================================================
-- V12: Agregar UNIQUE constraint en (local_id, numero) para tabla mesas
-- ============================================================================
-- Problema: la tabla mesas no tenía protección a nivel BD contra números 
-- duplicados dentro del mismo local. La validación existía solo a nivel
-- aplicativo (CrearMesaUseCase), lo cual es susceptible a race conditions.
--
-- Fix: Primero corregimos datos duplicados existentes renumerando, 
-- luego agregamos el constraint para prevenir futuros duplicados.
-- ============================================================================

-- 1. Corregir duplicados existentes antes de agregar el constraint.
--    Para cada local, si hay N filas con el mismo número, las renumera 
--    secuencialmente empezando desde MAX(numero)+1 del local.
DO $$
DECLARE
    dup RECORD;
    nuevo_numero INTEGER;
BEGIN
    FOR dup IN
        SELECT m.id, m.local_id, m.numero,
               ROW_NUMBER() OVER (PARTITION BY m.local_id, m.numero ORDER BY m.id) AS rn
        FROM mesas m
    LOOP
        -- Solo renumerar las filas duplicadas (rn > 1)
        IF dup.rn > 1 THEN
            SELECT COALESCE(MAX(numero), 0) + 1 INTO nuevo_numero
            FROM mesas
            WHERE local_id = dup.local_id;

            UPDATE mesas SET numero = nuevo_numero WHERE id = dup.id;

            RAISE NOTICE 'Mesa % renumerada de % a % (local: %)',
                dup.id, dup.numero, nuevo_numero, dup.local_id;
        END IF;
    END LOOP;
END $$;

-- 2. Agregar el UNIQUE constraint
ALTER TABLE mesas
    ADD CONSTRAINT uk_mesa_local_numero UNIQUE (local_id, numero);
