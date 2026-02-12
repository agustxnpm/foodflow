-- =====================================================
-- Seed para Test E2E: Reapertura de Pedido (HU-14)
-- =====================================================
-- Archivo: seed-reapertura-pedido.sql
-- Descripción: Datos de prueba para el flujo de reapertura de pedido cerrado
-- Productos: Hamburguesa Completa ($2800) + Cerveza Artesanal ($1200)
-- Mesa: Mesa 77 (LIBRE)
-- =====================================================

-- Limpiar datos previos de testing E2E
DELETE FROM items_pedido WHERE pedido_id IN (
    SELECT id FROM pedidos WHERE mesa_id = '1eab1e01-0000-0000-0000-000000000077'
);
DELETE FROM pedidos_pagos WHERE pedido_id IN (
    SELECT id FROM pedidos WHERE mesa_id = '1eab1e01-0000-0000-0000-000000000077'
);
DELETE FROM pedidos WHERE mesa_id = '1eab1e01-0000-0000-0000-000000000077';
DELETE FROM mesas WHERE id = '1eab1e01-0000-0000-0000-000000000077';
DELETE FROM productos WHERE id IN (
  '1eab1e10-0000-0000-0000-000000001001',
  '1eab1e10-0000-0000-0000-000000001002'
);

-- =====================================================
-- PRODUCTOS: Hamburguesa Completa + Cerveza Artesanal
-- =====================================================

-- Hamburguesa Completa ($2800)
INSERT INTO productos (id, local_id, nombre, precio, activo, color_hex, es_extra) VALUES (
  '1eab1e10-0000-0000-0000-000000001001',
  '123e4567-e89b-12d3-a456-426614174000',
  'Hamburguesa Completa',
  2800.00,
  true,
  '#FF6B35',
  false
);

-- Cerveza Artesanal ($1200)
INSERT INTO productos (id, local_id, nombre, precio, activo, color_hex, es_extra) VALUES (
  '1eab1e10-0000-0000-0000-000000001002',
  '123e4567-e89b-12d3-a456-426614174000',
  'Cerveza Artesanal',
  1200.00,
  true,
  '#F4A259',
  false
);

-- =====================================================
-- MESA: Mesa 77 para el flujo E2E de Reapertura
-- =====================================================
INSERT INTO mesas (id, local_id, numero, estado) VALUES (
  '1eab1e01-0000-0000-0000-000000000077',
  '123e4567-e89b-12d3-a456-426614174000',
  77,
  'LIBRE'
) ON CONFLICT (id) DO UPDATE SET estado = 'LIBRE';

-- =====================================================
-- FIN DEL SEED
-- =====================================================
-- Uso:
--   psql -U usuario -d comandas_db -f seed-reapertura-pedido.sql
--   o desde el proyecto Spring Boot con Flyway
-- =====================================================
