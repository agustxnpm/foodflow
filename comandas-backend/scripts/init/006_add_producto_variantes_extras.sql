-- =====================================================
-- HU-05.1 + HU-22: Agregar soporte para variantes y extras en productos
-- =====================================================
-- Fecha: 2026-02-09
-- Descripción: Agrega campos necesarios para gestionar variantes de productos
--              (ej: Hamburguesa Simple, Doble, Triple) y extras controlados
--              (ej: disco de carne, huevo, bacon)
-- =====================================================

-- Agregar campos a la tabla productos
ALTER TABLE productos
ADD COLUMN grupo_variante_id UUID NULL,
ADD COLUMN es_extra BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN cantidad_discos_carne INTEGER NULL;

-- Comentarios descriptivos
COMMENT ON COLUMN productos.grupo_variante_id IS 'Identifica productos hermanos de la misma variante (ej: todas las Hamburguesas Completa comparten el mismo ID)';
COMMENT ON COLUMN productos.es_extra IS 'Indica si el producto es un extra (huevo, queso, disco de carne, etc.)';
COMMENT ON COLUMN productos.cantidad_discos_carne IS 'Define la jerarquía de variantes para hamburguesas (Simple=1, Doble=2, Triple=3)';

-- Índice para búsqueda eficiente de variantes hermanas
CREATE INDEX idx_productos_grupo_variante_id 
ON productos(local_id, grupo_variante_id) 
WHERE grupo_variante_id IS NOT NULL;

-- Índice para búsqueda de extras
CREATE INDEX idx_productos_es_extra 
ON productos(local_id, es_extra) 
WHERE es_extra = TRUE;

-- Índice para búsqueda por cantidad de discos (útil para encontrar variante máxima)
CREATE INDEX idx_productos_cantidad_discos_carne 
ON productos(local_id, cantidad_discos_carne) 
WHERE cantidad_discos_carne IS NOT NULL;

-- Verificación de integridad
-- Si un producto es hamburguesa (tiene cantidad_discos_carne), debe tener grupo_variante_id
-- NOTA: No se agrega constraint CHECK para permitir flexibilidad en catálogos pequeños

-- Mensajes de confirmación para logs
DO $$
BEGIN
    RAISE NOTICE 'Migración 006 completada: Campos de variantes y extras agregados a productos';
    RAISE NOTICE 'Índices creados: idx_productos_grupo_variante_id, idx_productos_es_extra, idx_productos_cantidad_discos_carne';
END $$;
