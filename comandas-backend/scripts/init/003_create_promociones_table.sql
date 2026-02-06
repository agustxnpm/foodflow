-- ============================================
-- Creación de tabla: promociones
-- Refactor: uso de JSONB para triggers (criterios de activación)
-- ============================================

CREATE TABLE IF NOT EXISTS promociones (
    id UUID PRIMARY KEY,
    local_id UUID NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(500),
    prioridad INT NOT NULL,
    estado VARCHAR(20) NOT NULL,
    
    -- Estrategia (aplanada)
    tipo_estrategia VARCHAR(30) NOT NULL,
    modo_descuento VARCHAR(20),
    valor_descuento DECIMAL(10, 2),
    cantidad_llevas INT,
    cantidad_pagas INT,
    cantidad_minima_trigger INT,
    porcentaje_beneficio DECIMAL(5, 2),
    
    -- Triggers (JSONB)
    triggers_json TEXT NOT NULL,
    
    -- Constraints
    CONSTRAINT uk_promociones_nombre_local UNIQUE (nombre, local_id),
    CONSTRAINT chk_prioridad_positiva CHECK (prioridad > 0)
);

-- Índice por local_id para multi-tenancy
CREATE INDEX idx_promociones_local ON promociones(local_id);

-- Comentarios
COMMENT ON COLUMN promociones.triggers_json IS 'Array JSON de criterios de activación. Estructura: [{"tipo": "TEMPORAL", ...}, {"tipo": "CONTENIDO", ...}]';
COMMENT ON COLUMN promociones.tipo_estrategia IS 'Discriminador: DESCUENTO_DIRECTO, CANTIDAD_FIJA, COMBO_CONDICIONAL';
COMMENT ON TABLE promociones IS 'Promociones automáticas del local con triggers configurables (AND logic)';
