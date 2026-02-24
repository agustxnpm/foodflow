-- ============================================================================
-- Migración 009: Agregar flag requiere_configuracion a productos
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
ADD COLUMN requiere_configuracion BOOLEAN NOT NULL DEFAULT TRUE;

-- Los extras nunca requieren configuración propia (se agregan como complemento)
UPDATE productos SET requiere_configuracion = FALSE WHERE es_extra = TRUE;

COMMENT ON COLUMN productos.requiere_configuracion
IS 'Si es true, el POS abre el modal de observaciones/extras antes de agregar al pedido. Si es false, se agrega directamente.';
