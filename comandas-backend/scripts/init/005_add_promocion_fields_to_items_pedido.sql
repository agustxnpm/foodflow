-- Migración: Agregar campos de promoción a items_pedido
-- HU-10: Aplicar promociones automáticamente
-- Fecha: 2026-02-06
-- 
-- Descripción: Agrega los campos necesarios para el snapshot de promoción en cada ítem.
-- Estos campos almacenan el descuento calculado al momento de agregar el producto,
-- garantizando que cambios futuros en las promociones NO afecten ítems ya creados.

-- Agregar columna para el monto del descuento (valor monetario calculado)
ALTER TABLE items_pedido 
ADD COLUMN IF NOT EXISTS monto_descuento DECIMAL(10, 2) NOT NULL DEFAULT 0;

-- Agregar columna para el nombre de la promoción (para mostrar al cliente)
ALTER TABLE items_pedido 
ADD COLUMN IF NOT EXISTS nombre_promocion VARCHAR(150);

-- Agregar columna para el ID de la promoción (para auditoría y trazabilidad)
ALTER TABLE items_pedido 
ADD COLUMN IF NOT EXISTS promocion_id UUID;

-- Índice opcional para consultas de auditoría por promoción
CREATE INDEX IF NOT EXISTS idx_items_pedido_promocion_id 
ON items_pedido(promocion_id) 
WHERE promocion_id IS NOT NULL;

-- Comentarios descriptivos
COMMENT ON COLUMN items_pedido.monto_descuento IS 'Snapshot del monto de descuento calculado al momento de agregar el ítem (HU-10)';
COMMENT ON COLUMN items_pedido.nombre_promocion IS 'Nombre de la promoción aplicada para mostrar en ticket (HU-10)';
COMMENT ON COLUMN items_pedido.promocion_id IS 'ID de la promoción aplicada para auditoría (HU-10)';
