-- V6: Agrega categoría simple y flag permite_extras a productos.
--
-- categoria: etiqueta libre (ej: "bebida", "comida", "postre") que el frontend
--   usa para decidir si mostrar observaciones / extras en el POS.
--   Nullable porque no es obligatorio clasificar todos los productos.
--
-- permite_extras: flag que indica si el producto acepta extras/agregados.
--   Default true → la mayoría de los productos sí los aceptan.
--   Las bebidas y productos simples pueden tenerlo en false.

ALTER TABLE productos ADD COLUMN IF NOT EXISTS categoria VARCHAR(100);
ALTER TABLE productos ADD COLUMN IF NOT EXISTS permite_extras BOOLEAN DEFAULT TRUE NOT NULL;
