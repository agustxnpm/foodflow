-- ============================================================================
-- V13: Agregar relación categoría → categoría de modificadores
-- ============================================================================
--
-- Permite asociar una categoría de productos (ej: "Panchos") con una categoría
-- de modificadores específicos (ej: "Salsas para Panchos").
--
-- Cuando un producto pertenece a una categoría con este campo definido,
-- el modal de configuración del POS muestra EXCLUSIVAMENTE los productos
-- de la categoría de modificadores (en lugar de los extras genéricos).
--
-- Si el campo es NULL, el comportamiento actual (extras genéricos) se mantiene.
-- ============================================================================

ALTER TABLE categorias
    ADD COLUMN categoria_modificadores_id UUID NULL;

-- FK referencial a la propia tabla (auto-referencia)
ALTER TABLE categorias
    ADD CONSTRAINT fk_categoria_modificadores
    FOREIGN KEY (categoria_modificadores_id)
    REFERENCES categorias (id)
    ON DELETE SET NULL;

-- Índice parcial para consultas eficientes (solo filas con el campo poblado)
CREATE INDEX idx_categorias_modificadores_id
    ON categorias (categoria_modificadores_id)
    WHERE categoria_modificadores_id IS NOT NULL;
