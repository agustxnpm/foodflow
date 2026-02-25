-- V8: Agrega flag es_modificador_estructural a productos.
--
-- Motivación: Eliminar la detección del "disco de carne" por nombre literal (ILIKE '%disco%carne%').
-- Un modificador estructural es un extra que, al agregarse a un producto con variantes,
-- activa la normalización automática (ej: Simple + Disco → Doble).
--
-- La identificación ahora se hace por flag booleano, no por nombre.
-- Esto permite que cualquier extra se marque como modificador estructural,
-- independientemente de cómo se llame en el catálogo del local.

ALTER TABLE productos
    ADD COLUMN IF NOT EXISTS es_modificador_estructural BOOLEAN NOT NULL DEFAULT FALSE;

-- Auto-migración: marcar productos existentes que funcionaban como "disco de carne"
-- Esta es la ÚNICA vez que se usa nombre para identificar. Después de esta migración,
-- solo importa el flag.
UPDATE productos
SET es_modificador_estructural = true
WHERE es_extra = true
  AND LOWER(nombre) LIKE '%disco%carne%';
