-- Script de carga de datos para testing de PedidoController
-- Uso: ./ff staging scripts/staging/test-data.sql

-- ========================================
-- 1. PRODUCTOS DEL CATÁLOGO
-- ========================================

-- Productos del Meiser Sandwicheria
-- Nota: Usa el local ID '123e4567-e89b-12d3-a456-426614174000' que ya existe

-- Sandwiches
INSERT INTO productos (id, local_id, nombre, precio, activo) 
VALUES 
  ('11111111-1111-1111-1111-111111111111', '123e4567-e89b-12d3-a456-426614174000', 'Milanesa Completa', 1500.00, true),
  ('22222222-2222-2222-2222-222222222222', '123e4567-e89b-12d3-a456-426614174000', 'Jamón y Queso', 1200.00, true),
  ('33333333-3333-3333-3333-333333333333', '123e4567-e89b-12d3-a456-426614174000', 'Lomito Completo', 1800.00, true),
  ('44444444-4444-4444-4444-444444444444', '123e4567-e89b-12d3-a456-426614174000', 'Bondiola a la Pizza', 1600.00, true),
  ('55555555-5555-5555-5555-555555555555', '123e4567-e89b-12d3-a456-426614174000', 'Hamburguesa Simple', 1400.00, true)
ON CONFLICT (id) DO NOTHING;

-- Bebidas
INSERT INTO productos (id, local_id, nombre, precio, activo) 
VALUES 
  ('66666666-6666-6666-6666-666666666666', '123e4567-e89b-12d3-a456-426614174000', 'Coca Cola 500ml', 500.00, true),
  ('77777777-7777-7777-7777-777777777777', '123e4567-e89b-12d3-a456-426614174000', 'Cerveza Quilmes 1L', 800.00, true),
  ('88888888-8888-8888-8888-888888888888', '123e4567-e89b-12d3-a456-426614174000', 'Agua Mineral', 400.00, true)
ON CONFLICT (id) DO NOTHING;

-- Extras
INSERT INTO productos (id, local_id, nombre, precio, activo) 
VALUES 
  ('99999999-9999-9999-9999-999999999999', '123e4567-e89b-12d3-a456-426614174000', 'Papas Fritas Grandes', 900.00, true),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '123e4567-e89b-12d3-a456-426614174000', 'Papas Fritas Chicas', 600.00, true)
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- 2. PEDIDO ABIERTO EN MESA 5
-- ========================================

-- Crear un pedido abierto en la mesa 5 (que ya está marcada como ABIERTA)
-- Este será el pedido al que agregaremos productos via API
INSERT INTO pedidos (id, local_id, mesa_id, numero, estado, fecha_apertura) 
VALUES 
  ('00000011-1111-1111-1111-111111111111', 
   '123e4567-e89b-12d3-a456-426614174000',  -- local_id
   'a5555555-5555-5555-5555-555555555555',  -- mesa_id (mesa 5)
   1,                                        -- numero de pedido
   'ABIERTO',                               -- estado
   CURRENT_TIMESTAMP)                        -- fecha_apertura
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- 3. PEDIDO ABIERTO EN MESA 6 (vacío)
-- ========================================

-- Otro pedido para pruebas adicionales
INSERT INTO pedidos (id, local_id, mesa_id, numero, estado, fecha_apertura) 
VALUES 
  ('00000022-2222-2222-2222-222222222222', 
   '123e4567-e89b-12d3-a456-426614174000',
   'a6666666-6666-6666-6666-666666666666',  -- mesa_id (mesa 6)
   2,
   'ABIERTO',
   CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- 4. VERIFICACIÓN
-- ========================================

SELECT 
  'Datos cargados exitosamente:' as mensaje,
  (SELECT COUNT(*) FROM productos WHERE local_id = '123e4567-e89b-12d3-a456-426614174000') as productos_insertados,
  (SELECT COUNT(*) FROM pedidos WHERE local_id = '123e4567-e89b-12d3-a456-426614174000' AND estado = 'ABIERTO') as pedidos_abiertos;

-- ========================================
-- INFORMACIÓN ÚTIL PARA CURL TESTS
-- ========================================

-- Pedido ID para usar en requests:
--   00000011-1111-1111-1111-111111111111 (en mesa 5)
--   00000022-2222-2222-2222-222222222222 (en mesa 6)

-- Producto IDs disponibles:
--   11111111-1111-1111-1111-111111111111 (Milanesa Completa - $1500)
--   22222222-2222-2222-2222-222222222222 (Jamón y Queso - $1200)
--   33333333-3333-3333-3333-333333333333 (Lomito Completo - $1800)
--   44444444-4444-4444-4444-444444444444 (Bondiola a la Pizza - $1600)
--   55555555-5555-5555-5555-555555555555 (Hamburguesa Simple - $1400)
--   66666666-6666-6666-6666-666666666666 (Coca Cola - $500)
--   77777777-7777-7777-7777-777777777777 (Cerveza - $800)
--   88888888-8888-8888-8888-888888888888 (Agua - $400)
--   99999999-9999-9999-9999-999999999999 (Papas Grandes - $900)
--   aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa (Papas Chicas - $600)
