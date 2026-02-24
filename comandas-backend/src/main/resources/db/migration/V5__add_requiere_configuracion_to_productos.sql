-- ============================================================================
-- V5: Agregar flag requiere_configuracion a productos
-- ============================================================================
-- HU-XX: Control de flujo POS — productos configurables vs. agregado directo
--
-- Contexto:
--   El frontend POS necesita saber si un producto requiere abrir el modal
--   de configuración (observaciones + extras) o si se puede agregar
--   directamente al pedido con un solo toque.
--
-- Decisión de diseño:
--   - El backend es dueño de la decisión → el frontend NO interpreta el dominio
--   - El flag es un boolean explícito por producto
--   - Default TRUE para productos normales (preserva comportamiento actual)
--   - Default FALSE para extras (los extras no se configuran, se agregan a otros)
-- ============================================================================

ALTER TABLE productos
ADD COLUMN IF NOT EXISTS requiere_configuracion BOOLEAN NOT NULL DEFAULT TRUE;

-- Los extras no se configuran, se agregan directamente a otros productos
UPDATE productos SET requiere_configuracion = FALSE WHERE es_extra = TRUE;
