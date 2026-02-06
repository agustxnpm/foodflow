-- Script de migración para asociar productos a promociones (HU-09)
-- Fecha: 2026-02-06
-- Propósito: Crear tabla intermedia para el alcance de promociones

-- ============================================
-- Tabla: promocion_productos_scope
-- ============================================

CREATE TABLE IF NOT EXISTS promocion_productos_scope (
    id UUID NOT NULL PRIMARY KEY,
    promocion_id UUID NOT NULL,
    referencia_id UUID NOT NULL,
    tipo_alcance VARCHAR(20) NOT NULL CHECK (tipo_alcance IN ('PRODUCTO', 'CATEGORIA')),
    rol VARCHAR(20) NOT NULL CHECK (rol IN ('TRIGGER', 'TARGET')),
    
    CONSTRAINT fk_scope_promocion FOREIGN KEY (promocion_id) 
        REFERENCES promociones (id) ON DELETE CASCADE,
    
    -- Un producto/categoría puede aparecer solo una vez por promoción (sin duplicados de rol)
    CONSTRAINT uk_promocion_referencia UNIQUE (promocion_id, referencia_id)
);

-- Índices para optimizar consultas frecuentes
CREATE INDEX IF NOT EXISTS idx_scope_promocion_id ON promocion_productos_scope (promocion_id);
CREATE INDEX IF NOT EXISTS idx_scope_referencia_id ON promocion_productos_scope (referencia_id);
CREATE INDEX IF NOT EXISTS idx_scope_rol ON promocion_productos_scope (rol);

-- Comentarios para documentación
COMMENT ON TABLE promocion_productos_scope IS 'HU-09: Tabla intermedia que define el alcance (scope) de una promoción. Especifica qué productos/categorías activan la promoción (TRIGGER) y cuáles reciben el beneficio (TARGET).';
COMMENT ON COLUMN promocion_productos_scope.id IS 'Identificador único del ítem de alcance';
COMMENT ON COLUMN promocion_productos_scope.promocion_id IS 'ID de la promoción a la que pertenece este ítem';
COMMENT ON COLUMN promocion_productos_scope.referencia_id IS 'UUID del producto o categoría referenciado';
COMMENT ON COLUMN promocion_productos_scope.tipo_alcance IS 'Tipo de referencia: PRODUCTO (individual) o CATEGORIA (grupo)';
COMMENT ON COLUMN promocion_productos_scope.rol IS 'Rol en la promoción: TRIGGER (activa) o TARGET (recibe beneficio)';
