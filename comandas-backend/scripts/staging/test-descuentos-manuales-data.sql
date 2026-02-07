-- ============================================
-- Script de Carga de Datos para Testing HU-14
-- Suite: Descuentos Manuales
-- Fecha: 2026-02-06
-- ============================================
-- Uso: ./ff staging scripts/staging/test-descuentos-manuales-data.sql

-- ========================================
-- 1. PRODUCTOS ESPECÍFICOS PARA TESTS
-- ========================================

-- Productos con precios redondos para facilitar el testing de descuentos
INSERT INTO productos (id, local_id, nombre, precio, activo, color_hex) 
VALUES 
  ('d1111111-1111-1111-1111-111111111111', '123e4567-e89b-12d3-a456-426614174000', 'Pizza Muzzarella', 2500.00, true, '#FF5733'),
  ('d2222222-2222-2222-2222-222222222222', '123e4567-e89b-12d3-a456-426614174000', 'Empanadas x12', 3600.00, true, '#FFC300'),
  ('d3333333-3333-3333-3333-333333333333', '123e4567-e89b-12d3-a456-426614174000', 'Cerveza Artesanal', 1000.00, true, '#DAF7A6'),
  ('d4444444-4444-4444-4444-444444444444', '123e4567-e89b-12d3-a456-426614174000', 'Ensalada César', 1500.00, true, '#33FF57')
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- 2. MESAS DE PRUEBA
-- ========================================

-- Crear mesas si no existen
INSERT INTO mesas (id, local_id, numero, estado)
VALUES
  ('aa000010-0010-0010-0010-000000000010', '123e4567-e89b-12d3-a456-426614174000', 10, 'ABIERTA'),
  ('aa000011-0011-0011-0011-000000000011', '123e4567-e89b-12d3-a456-426614174000', 11, 'ABIERTA'),
  ('aa000012-0012-0012-0012-000000000012', '123e4567-e89b-12d3-a456-426614174000', 12, 'ABIERTA')
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- 3. PEDIDO CON ITEMS PARA TEST DESCUENTO GLOBAL
-- ========================================

-- Pedido en mesa 10 con 2 ítems
INSERT INTO pedidos (id, local_id, mesa_id, numero, estado, fecha_apertura) 
VALUES 
  ('dd000001-0001-0001-0001-000000000001', 
   '123e4567-e89b-12d3-a456-426614174000',
   'aa000010-0010-0010-0010-000000000010',  -- mesa 10
   101,
   'ABIERTO',
   CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Items del pedido
-- Subtotal: $2500 + $3600 = $6100
INSERT INTO items_pedido (
  id, 
  pedido_id, 
  producto_id, 
  nombre_producto, 
  precio_unitario, 
  cantidad,
  monto_descuento,
  observacion
) 
VALUES 
  ('dd000011-0011-0011-0011-000000000011',
   'dd000001-0001-0001-0001-000000000001',
   'd1111111-1111-1111-1111-111111111111',
   'Pizza Muzzarella',
   2500.00,
   1,
   0.00,
   NULL),
  ('dd000012-0012-0012-0012-000000000012',
   'dd000001-0001-0001-0001-000000000001',
   'd2222222-2222-2222-2222-222222222222',
   'Empanadas x12',
   3600.00,
   1,
   0.00,
   NULL)
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- 4. PEDIDO VACÍO PARA TEST AGREGAR ITEMS + DESCUENTO
-- ========================================

-- Pedido en mesa 11 vacío (para agregar items via API)
INSERT INTO pedidos (id, local_id, mesa_id, numero, estado, fecha_apertura) 
VALUES 
  ('dd000002-0002-0002-0002-000000000002', 
   '123e4567-e89b-12d3-a456-426614174000',
   'aa000011-0011-0011-0011-000000000011',  -- mesa 11
   102,
   'ABIERTO',
   CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- 5. PEDIDO CON 1 ÍTEM PARA TEST DESCUENTO POR ÍTEM
-- ========================================

-- Pedido en mesa 12 con 1 ítem
INSERT INTO pedidos (id, local_id, mesa_id, numero, estado, fecha_apertura) 
VALUES 
  ('dd000003-0003-0003-0003-000000000003', 
   '123e4567-e89b-12d3-a456-426614174000',
   'aa000012-0012-0012-0012-000000000012',  -- mesa 12
   103,
   'ABIERTO',
   CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Item del pedido (Cerveza $1000)
INSERT INTO items_pedido (
  id, 
  pedido_id, 
  producto_id, 
  nombre_producto, 
  precio_unitario, 
  cantidad,
  monto_descuento
) 
VALUES 
  ('dd000013-0013-0013-0013-000000000013',
   'dd000003-0003-0003-0003-000000000003',
   'd3333333-3333-3333-3333-333333333333',
   'Cerveza Artesanal',
   1000.00,
   2,
   0.00)  -- 2 cervezas = $2000
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- 6. USUARIO DE PRUEBA (MOZO)
-- ========================================

-- Usuario ID fijo para usar en requests
-- UUID: 550e8400-e29b-41d4-a716-446655440000
-- Nota: Este UUID debe usarse en el campo "usuarioId" de los requests

-- ========================================
-- 7. VERIFICACIÓN
-- ========================================

SELECT 
  '✓ Datos de prueba HU-14 cargados exitosamente' as mensaje,
  (SELECT COUNT(*) FROM productos WHERE id::text LIKE 'd%') as productos_test,
  (SELECT COUNT(*) FROM pedidos WHERE id::text LIKE 'dd%') as pedidos_test,
  (SELECT COUNT(*) FROM items_pedido WHERE id::text LIKE 'dd%') as items_test;

-- ========================================
-- INFORMACIÓN ÚTIL PARA CURL TESTS
-- ========================================

-- PEDIDO IDs (para usar en requests):
--   dd000001-0001-0001-0001-000000000001  → Mesa 10, con 2 items ($6100 total)
--   dd000002-0002-0002-0002-000000000002  → Mesa 11, vacío (para agregar items)
--   dd000003-0003-0003-0003-000000000003  → Mesa 12, 1 item ($2000 total)

-- ITEM IDs (para descuento por ítem):
--   dd000011-0011-0011-0011-000000000011  → Pizza $2500 (pedido dd000001)
--   dd000012-0012-0012-0012-000000000012  → Empanadas $3600 (pedido dd000001)
--   dd000013-0013-0013-0013-000000000013  → Cerveza x2 $2000 (pedido dd000003)

-- PRODUCTO IDs (para agregar items):
--   d1111111-1111-1111-1111-111111111111  → Pizza Muzzarella $2500
--   d2222222-2222-2222-2222-222222222222  → Empanadas x12 $3600
--   d3333333-3333-3333-3333-333333333333  → Cerveza Artesanal $1000
--   d4444444-4444-4444-4444-444444444444  → Ensalada César $1500

-- USUARIO ID (mozo):
--   550e8400-e29b-41d4-a716-446655440000

-- ========================================
-- ESCENARIOS DE PRUEBA SUGERIDOS
-- ========================================

-- Test 1: Descuento Global 10% sobre pedido de $6100
--   → Descuento esperado: $610
--   → Total final: $5490

-- Test 2: Descuento por Ítem 20% sobre Pizza de $2500
--   → Descuento esperado: $500
--   → Precio final del ítem: $2000

-- Test 3: Agregar ítem + aplicar descuento global
--   → Verificar dinamismo del descuento

-- Test 4: Aplicar descuento, luego sobrescribir con otro porcentaje
--   → Verificar que el nuevo reemplaza al anterior

-- Test 5: Descuento sobre ítem + descuento global
--   → Verificar jerarquía de descuentos
