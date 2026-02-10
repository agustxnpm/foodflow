-- V2: Arregla columna es_extra en productos
-- Contexto: La tabla productos ya existe con filas, pero es_extra falta o tiene NULLs
-- Esta migración es idempotente y segura

-- Paso 1: Agregar columna si no existe (con default temporal para evitar errores)
ALTER TABLE productos 
ADD COLUMN IF NOT EXISTS es_extra boolean DEFAULT false;

-- Paso 2: Rellenar NULLs con false (productos normales por defecto)
UPDATE productos 
SET es_extra = false 
WHERE es_extra IS NULL;

-- Paso 3: Hacer la columna NOT NULL ahora que no hay NULLs
ALTER TABLE productos 
ALTER COLUMN es_extra SET NOT NULL;

-- Paso 4: Quitar el default (queremos forzar valor explícito en inserts futuros)
ALTER TABLE productos 
ALTER COLUMN es_extra DROP DEFAULT;
