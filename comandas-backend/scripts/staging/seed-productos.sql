-- =============================================================================
-- Seed de datos de prueba: Categorías + Productos
-- Fecha: 2026-02-24
-- Uso:   ./ff staging scripts/staging/seed-productos.sql
--
-- Refleja el modelo post-V7: cada producto apunta a una categoría por FK.
-- Incluye flags de dominio: admiteVariantes, esCategoriaExtra, permiteExtras,
-- requiereConfiguracion, esExtra, grupoVarianteId, cantidadDiscosCarne.
-- =============================================================================

-- ── Local de referencia ─────────────────────────────────────────────────────

-- Se asume que ya existe (seed-example.sql lo crea)
-- ID: 123e4567-e89b-12d3-a456-426614174000 = "Meiser Sandwichería"

-- ── Limpiar datos existentes (orden por FK) ─────────────────────────────────

DELETE FROM items_pedido_extras
  WHERE item_pedido_id IN (
    SELECT ip.id FROM items_pedido ip
    JOIN pedidos p ON ip.pedido_id = p.id
    WHERE p.local_id = '123e4567-e89b-12d3-a456-426614174000'
  );
DELETE FROM items_pedido
  WHERE pedido_id IN (
    SELECT id FROM pedidos WHERE local_id = '123e4567-e89b-12d3-a456-426614174000'
  );
DELETE FROM productos WHERE local_id = '123e4567-e89b-12d3-a456-426614174000';
DELETE FROM categorias WHERE local_id = '123e4567-e89b-12d3-a456-426614174000';

-- ═════════════════════════════════════════════════════════════════════════════
-- CATEGORÍAS
-- ═════════════════════════════════════════════════════════════════════════════
-- UUIDs fijos para referenciabilidad en seeds y tests.

INSERT INTO categorias (id, local_id, nombre, color_hex, admite_variantes, es_categoria_extra, orden) VALUES
  -- Hamburguesas: admite variantes (simple/doble/triple), no es extra
  ('ca000001-0000-0000-0000-000000000001', '123e4567-e89b-12d3-a456-426614174000',
   'Hamburguesas', '#FF0000', true, false, 1),

  -- Platos: no admite variantes, no es extra
  ('ca000001-0000-0000-0000-000000000002', '123e4567-e89b-12d3-a456-426614174000',
   'Platos', '#00FF00', false, false, 2),

  -- Bebidas: no admite variantes, no es extra
  ('ca000001-0000-0000-0000-000000000003', '123e4567-e89b-12d3-a456-426614174000',
   'Bebidas', '#0000FF', false, false, 3),

  -- Acompañamientos: no admite variantes, no es extra
  ('ca000001-0000-0000-0000-000000000004', '123e4567-e89b-12d3-a456-426614174000',
   'Acompañamientos', '#FFFF00', false, false, 4),

  -- Postres: no admite variantes, no es extra
  ('ca000001-0000-0000-0000-000000000005', '123e4567-e89b-12d3-a456-426614174000',
   'Postres', '#FFA500', false, false, 5),

  -- Extras: es categoría de extras (huevo, queso, medallón, etc.)
  ('ca000001-0000-0000-0000-000000000006', '123e4567-e89b-12d3-a456-426614174000',
   'Extras', '#FF69B4', false, true, 6);


-- ═════════════════════════════════════════════════════════════════════════════
-- PRODUCTOS
-- ═════════════════════════════════════════════════════════════════════════════
--
-- Convenciones:
--   - color_hex del producto = color_hex de su categoría
--   - grupo_variante_id: mismo UUID agrupa variantes de un producto base
--   - cantidad_discos_carne: ordena variantes (1=simple, 2=doble, 3=triple)
--   - requiere_configuracion: true abre modal (observaciones + extras)
--   - permite_extras: true muestra selector de extras en el modal
--   - es_extra: true marca al producto como un agregado

-- ── 1. HAMBURGUESAS ─────────────────────────────────────────────────────────
-- Admiten variantes (simple/doble/triple), requieren configuración, admiten extras

-- Hamburguesa Clásica (grupo variante)
INSERT INTO productos (id, local_id, nombre, precio, activo, color_hex, es_extra, permite_extras, requiere_configuracion, grupo_variante_id, cantidad_discos_carne, categoria_id) VALUES
('11111111-1111-1111-1111-111111111111', '123e4567-e89b-12d3-a456-426614174000',
 'Hamburguesa Clásica Simple', 1500.00, true, '#FF0000',
 false, true, true,
 'a0000001-0000-0000-0000-000000000001', 1,
 'ca000001-0000-0000-0000-000000000001'),

('11111111-1111-1111-1111-111111111112', '123e4567-e89b-12d3-a456-426614174000',
 'Hamburguesa Clásica Doble', 2000.00, true, '#FF0000',
 false, true, true,
 'a0000001-0000-0000-0000-000000000001', 2,
 'ca000001-0000-0000-0000-000000000001'),

('11111111-1111-1111-1111-111111111113', '123e4567-e89b-12d3-a456-426614174000',
 'Hamburguesa Clásica Triple', 2500.00, true, '#FF0000',
 false, true, true,
 'a0000001-0000-0000-0000-000000000001', 3,
 'ca000001-0000-0000-0000-000000000001');

-- Hamburguesa Especial (grupo variante distinto)
INSERT INTO productos (id, local_id, nombre, precio, activo, color_hex, es_extra, permite_extras, requiere_configuracion, grupo_variante_id, cantidad_discos_carne, categoria_id) VALUES
('11111111-1111-1111-1111-111111111121', '123e4567-e89b-12d3-a456-426614174000',
 'Hamburguesa Especial Simple', 1800.00, true, '#FF0000',
 false, true, true,
 'a0000001-0000-0000-0000-000000000002', 1,
 'ca000001-0000-0000-0000-000000000001'),

('11111111-1111-1111-1111-111111111122', '123e4567-e89b-12d3-a456-426614174000',
 'Hamburguesa Especial Doble', 2400.00, true, '#FF0000',
 false, true, true,
 'a0000001-0000-0000-0000-000000000002', 2,
 'ca000001-0000-0000-0000-000000000001');


-- ── 2. PLATOS ───────────────────────────────────────────────────────────────
-- Sin variantes, requieren configuración (observaciones), admiten extras

INSERT INTO productos (id, local_id, nombre, precio, activo, color_hex, es_extra, permite_extras, requiere_configuracion, categoria_id) VALUES
('22222222-2222-2222-2222-222222222221', '123e4567-e89b-12d3-a456-426614174000',
 'Milanesa Napolitana', 1800.00, true, '#00FF00',
 false, true, true,
 'ca000001-0000-0000-0000-000000000002'),

('22222222-2222-2222-2222-222222222222', '123e4567-e89b-12d3-a456-426614174000',
 'Ensalada César', 1200.00, true, '#00FF00',
 false, true, true,
 'ca000001-0000-0000-0000-000000000002'),

('22222222-2222-2222-2222-222222222223', '123e4567-e89b-12d3-a456-426614174000',
 'Pizza Muzzarella', 2000.00, true, '#00FF00',
 false, true, true,
 'ca000001-0000-0000-0000-000000000002');


-- ── 3. BEBIDAS ──────────────────────────────────────────────────────────────
-- Sin variantes, NO requieren configuración, NO admiten extras
-- (se agregan directo al pedido sin abrir modal)

INSERT INTO productos (id, local_id, nombre, precio, activo, color_hex, es_extra, permite_extras, requiere_configuracion, categoria_id) VALUES
('33333333-3333-3333-3333-333333333331', '123e4567-e89b-12d3-a456-426614174000',
 'Cerveza Artesanal', 800.00, true, '#0000FF',
 false, false, false,
 'ca000001-0000-0000-0000-000000000003'),

('33333333-3333-3333-3333-333333333332', '123e4567-e89b-12d3-a456-426614174000',
 'Agua Mineral', 300.00, true, '#0000FF',
 false, false, false,
 'ca000001-0000-0000-0000-000000000003'),

('33333333-3333-3333-3333-333333333333', '123e4567-e89b-12d3-a456-426614174000',
 'Gaseosa', 400.00, true, '#0000FF',
 false, false, false,
 'ca000001-0000-0000-0000-000000000003'),

('33333333-3333-3333-3333-333333333334', '123e4567-e89b-12d3-a456-426614174000',
 'Café Espresso', 350.00, true, '#0000FF',
 false, false, false,
 'ca000001-0000-0000-0000-000000000003');


-- ── 4. ACOMPAÑAMIENTOS ──────────────────────────────────────────────────────
-- Sin variantes, requieren configuración (observaciones), admiten extras

INSERT INTO productos (id, local_id, nombre, precio, activo, color_hex, es_extra, permite_extras, requiere_configuracion, categoria_id) VALUES
('44444444-4444-4444-4444-444444444441', '123e4567-e89b-12d3-a456-426614174000',
 'Papas Fritas', 700.00, true, '#FFFF00',
 false, true, true,
 'ca000001-0000-0000-0000-000000000004'),

('44444444-4444-4444-4444-444444444442', '123e4567-e89b-12d3-a456-426614174000',
 'Empanadas (x6)', 1000.00, true, '#FFFF00',
 false, false, true,
 'ca000001-0000-0000-0000-000000000004');


-- ── 5. POSTRES ──────────────────────────────────────────────────────────────
-- Sin variantes, NO admiten extras, NO requieren configuración

INSERT INTO productos (id, local_id, nombre, precio, activo, color_hex, es_extra, permite_extras, requiere_configuracion, categoria_id) VALUES
('55555555-5555-5555-5555-555555555551', '123e4567-e89b-12d3-a456-426614174000',
 'Tiramisu', 950.00, true, '#FFA500',
 false, false, false,
 'ca000001-0000-0000-0000-000000000005'),

('55555555-5555-5555-5555-555555555552', '123e4567-e89b-12d3-a456-426614174000',
 'Flan Casero', 600.00, true, '#FFA500',
 false, false, false,
 'ca000001-0000-0000-0000-000000000005'),

('55555555-5555-5555-5555-555555555553', '123e4567-e89b-12d3-a456-426614174000',
 'Helado (1 bocha)', 450.00, true, '#FFA500',
 false, false, false,
 'ca000001-0000-0000-0000-000000000005');


-- ── 6. EXTRAS ───────────────────────────────────────────────────────────────
-- Productos que se agregan como extras a otros (huevo, queso, medallón)
-- es_extra=true, NO requieren configuración, NO admiten extras ellos mismos
-- es_modificador_estructural=true → el extra activa normalización de variantes

INSERT INTO productos (id, local_id, nombre, precio, activo, color_hex, es_extra, es_modificador_estructural, permite_extras, requiere_configuracion, categoria_id) VALUES
('66666666-6666-6666-6666-666666666661', '123e4567-e89b-12d3-a456-426614174000',
 'Huevo Frito', 200.00, true, '#FF69B4',
 true, false, false, false,
 'ca000001-0000-0000-0000-000000000006'),

('66666666-6666-6666-6666-666666666662', '123e4567-e89b-12d3-a456-426614174000',
 'Queso Cheddar', 250.00, true, '#FF69B4',
 true, false, false, false,
 'ca000001-0000-0000-0000-000000000006'),

('66666666-6666-6666-6666-666666666663', '123e4567-e89b-12d3-a456-426614174000',
 'Medallón Extra', 400.00, true, '#FF69B4',
 true, true, false, false,
 'ca000001-0000-0000-0000-000000000006'),

('66666666-6666-6666-6666-666666666664', '123e4567-e89b-12d3-a456-426614174000',
 'Bacon', 300.00, true, '#FF69B4',
 true, false, false, false,
 'ca000001-0000-0000-0000-000000000006');


-- ── PRODUCTO INACTIVO (para testing) ────────────────────────────────────────

INSERT INTO productos (id, local_id, nombre, precio, activo, color_hex, es_extra, permite_extras, requiere_configuracion, categoria_id) VALUES
('99999999-9999-9999-9999-999999999999', '123e4567-e89b-12d3-a456-426614174000',
 'Producto Descontinuado', 100.00, false, '#708090',
 false, false, false,
 'ca000001-0000-0000-0000-000000000002');


-- ═════════════════════════════════════════════════════════════════════════════
-- VERIFICACIÓN
-- ═════════════════════════════════════════════════════════════════════════════

SELECT '── Categorías ──' AS seccion;
SELECT c.nombre, c.color_hex, c.admite_variantes, c.es_categoria_extra, c.orden,
       COUNT(p.id) AS productos
FROM categorias c
LEFT JOIN productos p ON p.categoria_id = c.id
WHERE c.local_id = '123e4567-e89b-12d3-a456-426614174000'
GROUP BY c.id, c.nombre, c.color_hex, c.admite_variantes, c.es_categoria_extra, c.orden
ORDER BY c.orden;

SELECT '── Productos ──' AS seccion;
SELECT
  COUNT(*) AS total,
  COUNT(*) FILTER (WHERE activo) AS activos,
  COUNT(*) FILTER (WHERE NOT activo) AS inactivos,
  COUNT(*) FILTER (WHERE es_extra) AS extras,
  COUNT(*) FILTER (WHERE es_modificador_estructural) AS modificadores_estructurales,
  COUNT(*) FILTER (WHERE categoria_id IS NOT NULL) AS con_categoria,
  COUNT(*) FILTER (WHERE grupo_variante_id IS NOT NULL) AS con_variantes
FROM productos
WHERE local_id = '123e4567-e89b-12d3-a456-426614174000';
