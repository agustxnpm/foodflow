-- =====================================================
-- HU-05.1 + HU-22: Tabla de extras para ítems de pedido
-- =====================================================
-- Fecha: 2026-02-09
-- Descripción: Crea la tabla que almacena los extras agregados a cada ítem de pedido.
--              Los extras capturan un snapshot del precio al momento de agregarse (patrón snapshot).
--              Ejemplos: Hamburguesa Simple + Huevo + Bacon
-- =====================================================

CREATE TABLE items_pedido_extras (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,
    
    -- Foreign key al ítem de pedido
    item_pedido_id UUID NOT NULL,
    
    -- Snapshot del extra al momento de agregarlo
    producto_id UUID NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    precio_snapshot NUMERIC(12, 2) NOT NULL,
    
    -- Auditoría
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_items_pedido_extras_item 
        FOREIGN KEY (item_pedido_id) 
        REFERENCES items_pedido(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT chk_precio_snapshot_positivo 
        CHECK (precio_snapshot >= 0)
);

-- Comentarios descriptivos
COMMENT ON TABLE items_pedido_extras IS 'Extras agregados a ítems de pedido con snapshot de precio (patrón snapshot)';
COMMENT ON COLUMN items_pedido_extras.item_pedido_id IS 'ID del ítem de pedido al que pertenece este extra';
COMMENT ON COLUMN items_pedido_extras.producto_id IS 'ID del producto extra en el catálogo (para auditoría)';
COMMENT ON COLUMN items_pedido_extras.nombre IS 'Snapshot del nombre del extra al momento de agregarlo';
COMMENT ON COLUMN items_pedido_extras.precio_snapshot IS 'Snapshot del precio del extra (NO se modifica si cambia el catálogo)';

-- Índice para consultas por ítem de pedido
CREATE INDEX idx_items_pedido_extras_item_pedido_id 
ON items_pedido_extras(item_pedido_id);

-- Índice para análisis de productos más usados como extras
CREATE INDEX idx_items_pedido_extras_producto_id 
ON items_pedido_extras(producto_id);

-- Reglas de negocio documentadas:
-- 1. Los extras NUNCA participan en cálculos de promociones (aislamiento)
-- 2. El precio_snapshot es inmutable (patrón snapshot histórico)
-- 3. La eliminación del ítem (ON DELETE CASCADE) elimina automáticamente sus extras
-- 4. No hay límite de cantidad de extras por ítem (decisión de negocio)

-- Mensajes de confirmación
DO $$
BEGIN
    RAISE NOTICE 'Migración 007 completada: Tabla items_pedido_extras creada';
    RAISE NOTICE 'Índices creados: idx_items_pedido_extras_item_pedido_id, idx_items_pedido_extras_producto_id';
    RAISE NOTICE 'Constraint ON DELETE CASCADE configurado para limpieza automática';
END $$;
