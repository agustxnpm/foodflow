-- HU-14: Aplicar descuento inmediato por porcentaje
--
-- Agrega columnas para soportar descuentos manuales dinámicos tanto a nivel de ítem
-- como a nivel de pedido completo.
--
-- Características:
-- - Descuento manual por ítem: afecta un producto específico
-- - Descuento global: afecta el total del pedido
-- - Solo se guarda el porcentaje (dinámico), NO el monto calculado
-- - Auditable: registra quién y cuándo aplicó el descuento

-- ============================================
-- Descuentos manuales a nivel de ÍTEM
-- ============================================

ALTER TABLE items_pedido ADD COLUMN desc_manual_porcentaje DECIMAL(5,2);
ALTER TABLE items_pedido ADD COLUMN desc_manual_razon VARCHAR(255);
ALTER TABLE items_pedido ADD COLUMN desc_manual_usuario_id UUID;
ALTER TABLE items_pedido ADD COLUMN desc_manual_fecha TIMESTAMP;

COMMENT ON COLUMN items_pedido.desc_manual_porcentaje IS 'HU-14: Porcentaje del descuento manual aplicado al ítem (0-100). NULL si no tiene descuento.';
COMMENT ON COLUMN items_pedido.desc_manual_razon IS 'HU-14: Motivo del descuento manual (ej: "Cliente frecuente").';
COMMENT ON COLUMN items_pedido.desc_manual_usuario_id IS 'HU-14: ID del usuario que aplicó el descuento (auditoría).';
COMMENT ON COLUMN items_pedido.desc_manual_fecha IS 'HU-14: Timestamp de aplicación del descuento.';

-- ============================================
-- Descuentos globales a nivel de PEDIDO
-- ============================================

ALTER TABLE pedidos ADD COLUMN desc_global_porcentaje DECIMAL(5,2);
ALTER TABLE pedidos ADD COLUMN desc_global_razon VARCHAR(255);
ALTER TABLE pedidos ADD COLUMN desc_global_usuario_id UUID;
ALTER TABLE pedidos ADD COLUMN desc_global_fecha TIMESTAMP;

COMMENT ON COLUMN pedidos.desc_global_porcentaje IS 'HU-14: Porcentaje del descuento global aplicado al pedido (0-100). NULL si no tiene descuento.';
COMMENT ON COLUMN pedidos.desc_global_razon IS 'HU-14: Motivo del descuento global.';
COMMENT ON COLUMN pedidos.desc_global_usuario_id IS 'HU-14: ID del usuario que aplicó el descuento global (auditoría).';
COMMENT ON COLUMN pedidos.desc_global_fecha IS 'HU-14: Timestamp de aplicación del descuento global.';

-- ============================================
-- Índices para optimizar consultas de auditoría
-- ============================================

-- Permite buscar rápidamente descuentos aplicados por un usuario específico
CREATE INDEX idx_items_descuento_usuario ON items_pedido(desc_manual_usuario_id) WHERE desc_manual_usuario_id IS NOT NULL;
CREATE INDEX idx_pedidos_descuento_usuario ON pedidos(desc_global_usuario_id) WHERE desc_global_usuario_id IS NOT NULL;

-- Permite buscar descuentos aplicados en un rango de fechas
CREATE INDEX idx_items_descuento_fecha ON items_pedido(desc_manual_fecha) WHERE desc_manual_fecha IS NOT NULL;
CREATE INDEX idx_pedidos_descuento_fecha ON pedidos(desc_global_fecha) WHERE desc_global_fecha IS NOT NULL;
