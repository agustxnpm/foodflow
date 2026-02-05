-- Migración: Crear tabla items_pedido
-- Fecha: 2026-02-04
-- Descripción: Almacena los ítems de cada pedido con snapshot de precio y nombre

CREATE TABLE IF NOT EXISTS items_pedido (
    id UUID PRIMARY KEY,
    pedido_id UUID NOT NULL,
    producto_id UUID NOT NULL,
    nombre_producto VARCHAR(100) NOT NULL,
    cantidad INT NOT NULL CHECK (cantidad > 0),
    precio_unitario DECIMAL(10, 2) NOT NULL CHECK (precio_unitario >= 0),
    observacion VARCHAR(255),
    
    -- Foreign key al pedido
    CONSTRAINT fk_item_pedido FOREIGN KEY (pedido_id) 
        REFERENCES pedidos(id) ON DELETE CASCADE,
    
    -- Índices para mejorar rendimiento
    CONSTRAINT idx_item_pedido_id UNIQUE (id)
);

-- Índice para consultar items por pedido
CREATE INDEX IF NOT EXISTS idx_items_pedido_pedido_id ON items_pedido(pedido_id);

-- Comentarios descriptivos
COMMENT ON TABLE items_pedido IS 'Ítems de pedidos con snapshot de precio y nombre del producto';
COMMENT ON COLUMN items_pedido.nombre_producto IS 'Snapshot del nombre del producto al momento de agregar al pedido';
COMMENT ON COLUMN items_pedido.precio_unitario IS 'Snapshot del precio del producto al momento de agregar al pedido';
COMMENT ON COLUMN items_pedido.observacion IS 'Notas adicionales del cliente (ej: sin cebolla, con mayonesa, etc)';
