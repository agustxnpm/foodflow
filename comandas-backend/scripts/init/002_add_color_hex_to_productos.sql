-- Migración: Agregar columna color_hex a la tabla productos
-- Fecha: 2026-02-05
-- Historia de Usuario: HU-19 - Gestión de Productos con Filtrado por Color

-- Agregar columna color_hex (formato hexadecimal #RRGGBB)
ALTER TABLE productos
ADD COLUMN color_hex VARCHAR(7);

-- Actualizar productos existentes con color por defecto (blanco)
UPDATE productos
SET color_hex = '#FFFFFF'
WHERE color_hex IS NULL;

-- Comentario descriptivo de la columna
COMMENT ON COLUMN productos.color_hex IS 'Código de color hexadecimal para identificación visual (#RRGGBB). Normalizado a mayúsculas.';
