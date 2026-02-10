-- =====================================================
-- Seed para Test E2E: Cierre de Mesa con Pago Split
-- =====================================================
-- Archivo: seed-e2e-cierre-mesa.sql
-- Descripción: Datos de prueba para el flujo completo de cierre de mesa
-- Productos: Hamburguesa Completa ($2500) + Papas Fritas ($800)
-- Mesa: Mesa 99 (LIBRE)
-- =====================================================

-- Limpiar datos previos de testing E2E
DELETE FROM items_pedido WHERE pedido_id IN (
    SELECT id FROM pedidos WHERE mesa_id = 'e2e00001-0001-0001-0001-000000000001'
);
DELETE FROM pedidos WHERE mesa_id = 'e2e00001-0001-0001-0001-000000000001';
DELETE FROM mesas WHERE id = 'e2e00001-0001-0001-0001-000000000001';
DELETE FROM productos WHERE id IN (
  'e2e10001-0001-0001-0001-000000000001',
  'e2e10002-0002-0002-0002-000000000002'
);

-- =====================================================
-- PRODUCTOS: Hamburguesa Completa + Papas Fritas
-- =====================================================

-- Hamburguesa Completa ($2500)
INSERT INTO productos (id, local_id, nombre, precio, activo, color_hex, es_extra) VALUES (
  'e2e10001-0001-0001-0001-000000000001',
  '123e4567-e89b-12d3-a456-426614174000',
  'Hamburguesa Completa',
  2500.00,
  true,
  '#FF5733',
  false
);

-- Papas Fritas ($800)
INSERT INTO productos (id, local_id, nombre, precio, activo, color_hex, es_extra) VALUES (
  'e2e10002-0002-0002-0002-000000000002',
  '123e4567-e89b-12d3-a456-426614174000',
  'Papas Fritas',
  800.00,
  true,
  '#FFD700',
  false
);

-- =====================================================
-- MESA: Mesa 99 para el flujo E2E
-- =====================================================
INSERT INTO mesas (id, local_id, numero, estado) VALUES (
  'e2e00001-0001-0001-0001-000000000001',
  '123e4567-e89b-12d3-a456-426614174000',
  99,
  'LIBRE'
);
