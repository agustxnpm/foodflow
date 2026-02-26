-- HU-14: Soporte para descuento manual por monto fijo
-- Agrega el campo tipo (PORCENTAJE | MONTO_FIJO) y renombra porcentaje → valor
-- ya que el campo ahora puede almacenar un porcentaje o un monto monetario.

-- ============================================
-- Tabla items_pedido: descuento manual por ítem
-- ============================================

-- Renombrar columna porcentaje a valor (el campo ahora es polimórfico)
ALTER TABLE items_pedido RENAME COLUMN desc_manual_porcentaje TO desc_manual_valor;

-- Ampliar precisión: antes DECIMAL(5,2) bastaba para %, ahora necesita montos monetarios
ALTER TABLE items_pedido ALTER COLUMN desc_manual_valor TYPE DECIMAL(10,2);

-- Agregar tipo de descuento manual
ALTER TABLE items_pedido ADD COLUMN desc_manual_tipo VARCHAR(20);

-- Migrar datos existentes: todos los descuentos previos son de tipo PORCENTAJE
UPDATE items_pedido SET desc_manual_tipo = 'PORCENTAJE' WHERE desc_manual_valor IS NOT NULL;

-- ============================================
-- Tabla pedidos: descuento global
-- ============================================

-- Renombrar columna porcentaje a valor
ALTER TABLE pedidos RENAME COLUMN desc_global_porcentaje TO desc_global_valor;

-- Ampliar precisión: antes DECIMAL(5,2) bastaba para %, ahora necesita montos monetarios
ALTER TABLE pedidos ALTER COLUMN desc_global_valor TYPE DECIMAL(10,2);

-- Agregar tipo de descuento global
ALTER TABLE pedidos ADD COLUMN desc_global_tipo VARCHAR(20);

-- Migrar datos existentes: todos los descuentos previos son de tipo PORCENTAJE
UPDATE pedidos SET desc_global_tipo = 'PORCENTAJE' WHERE desc_global_valor IS NOT NULL;
