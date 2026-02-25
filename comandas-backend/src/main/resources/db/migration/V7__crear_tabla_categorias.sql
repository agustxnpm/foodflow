-- V7: Crear tabla de categorías y migrar productos.categoria String → categoría FK.
--
-- Decisión de diseño: Categoria pasa de etiqueta libre (String) a entidad de dominio.
-- Esto permite al usuario crear categorías con comportamiento definido:
--   - admite_variantes: si los productos de esta categoría tienen variantes estructurales
--   - es_categoria_extra: si los productos de esta categoría son extras (huevo, queso, etc.)
--   - orden: posición visual en el frontend
--   - color_hex: color de la sección/tab en la UI
--
-- El sistema arranca sin datos: el usuario crea categorías antes de operar.

CREATE TABLE IF NOT EXISTS categorias (
    id UUID PRIMARY KEY,
    local_id UUID NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    color_hex VARCHAR(7) DEFAULT '#FFFFFF',
    admite_variantes BOOLEAN NOT NULL DEFAULT FALSE,
    es_categoria_extra BOOLEAN NOT NULL DEFAULT FALSE,
    orden INTEGER NOT NULL DEFAULT 0,
    UNIQUE(local_id, nombre)
);

-- Eliminar la columna legacy de categoría string
ALTER TABLE productos DROP COLUMN IF EXISTS categoria;

-- Agregar FK a la nueva tabla de categorías (nullable para retrocompatibilidad)
ALTER TABLE productos ADD COLUMN IF NOT EXISTS categoria_id UUID REFERENCES categorias(id);
