-- ============================================
-- Script de Carga de Datos para Testing HU-20 y HU-21
-- Suite: Gestionar Items de Pedido (Eliminar y Modificar Cantidad)
-- Fecha: 2026-02-07
-- ============================================
-- Uso: ./ff staging scripts/staging/test-gestionar-items-data.sql

-- ========================================
-- 1. PRODUCTOS ESPECÍFICOS PARA TESTS
-- ========================================

-- Productos con precios redondos para facilitar el testing
INSERT INTO productos (id, local_id, nombre, precio, activo, color_hex) 
VALUES 
  ('e1111111-1111-1111-1111-111111111111', '123e4567-e89b-12d3-a456-426614174000', 'Hamburguesa Completa', 2500.00, true, '#FF5733'),
  ('e2222222-2222-2222-2222-222222222222', '123e4567-e89b-12d3-a456-426614174000', 'Papas Fritas', 800.00, true, '#FFC300'),
  ('e3333333-3333-3333-3333-333333333333', '123e4567-e89b-12d3-a456-426614174000', 'Cerveza Quilmes', 1500.00, true, '#DAF7A6'),
  ('e4444444-4444-4444-4444-444444444444', '123e4567-e89b-12d3-a456-426614174000', 'Gaseosa Coca Cola', 600.00, true, '#33FF57'),
  ('e5555555-5555-5555-5555-555555555555', '123e4567-e89b-12d3-a456-426614174000', 'Milanesa Napolitana', 3000.00, true, '#3357FF')
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- 2. PROMOCIONES PARA TESTS DE RECÁLCULO
-- ========================================

-- Promoción 2x1 en Cervezas (para test de ciclos NxM)
INSERT INTO promociones (
  id, 
  local_id, 
  nombre, 
  descripcion, 
  prioridad, 
  estado,
  tipo_estrategia,
  cantidad_llevas,
  cantidad_pagas,
  triggers_json
) 
VALUES 
  ('e0000001-0001-0001-0001-000000000001',
   '123e4567-e89b-12d3-a456-426614174000',
   '2x1 Cervezas',
   'Llevá 2 cervezas y pagá solo 1',
   10,
   'ACTIVA',
   'CANTIDAD_FIJA',
   2,
   1,
   '[{"tipo":"TEMPORAL","fechaDesde":"2026-02-01","fechaHasta":"2026-12-31","diasSemana":null,"horaDesde":null,"horaHasta":null}]'::jsonb)
ON CONFLICT (id) DO NOTHING;

-- Alcance de la promoción 2x1 (producto target: Cerveza)
INSERT INTO promocion_productos_scope (id, promocion_id, referencia_id, tipo_alcance, rol)
VALUES
  ('e0000011-0011-0011-0011-000000000011',
   'e0000001-0001-0001-0001-000000000001',
   'e3333333-3333-3333-3333-333333333333',
   'PRODUCTO',
   'TARGET')
ON CONFLICT (id) DO NOTHING;

-- Promoción Combo: Hamburguesa + Papas 50% desc en papas (para test de rotura de combo)
INSERT INTO promociones (
  id, 
  local_id, 
  nombre, 
  descripcion, 
  prioridad, 
  estado,
  tipo_estrategia,
  cantidad_minima_trigger,
  porcentaje_beneficio,
  triggers_json
) 
VALUES 
  ('e0000002-0002-0002-0002-000000000002',
   '123e4567-e89b-12d3-a456-426614174000',
   'Combo Hamburguesa + Papas',
   'Llevá hamburguesa y las papas tienen 50% de descuento',
   20,
   'ACTIVA',
   'COMBO_CONDICIONAL',
   1,
   50.00,
   '[{"tipo":"TEMPORAL","fechaDesde":"2026-02-01","fechaHasta":"2026-12-31","diasSemana":null,"horaDesde":null,"horaHasta":null}]'::jsonb)
ON CONFLICT (id) DO NOTHING;

-- Alcance del combo (trigger: Hamburguesa, target: Papas)
INSERT INTO promocion_productos_scope (id, promocion_id, referencia_id, tipo_alcance, rol)
VALUES
  ('e0000021-0021-0021-0021-000000000021',
   'e0000002-0002-0002-0002-000000000002',
   'e1111111-1111-1111-1111-111111111111',
   'PRODUCTO',
   'TRIGGER'),
  ('e0000022-0022-0022-0022-000000000022',
   'e0000002-0002-0002-0002-000000000002',
   'e2222222-2222-2222-2222-222222222222',
   'PRODUCTO',
   'TARGET')
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- 3. MESAS DE PRUEBA
-- ========================================

INSERT INTO mesas (id, local_id, numero, estado)
VALUES
  ('ee000020-0020-0020-0020-000000000020', '123e4567-e89b-12d3-a456-426614174000', 20, 'ABIERTA'),
  ('ee000021-0021-0021-0021-000000000021', '123e4567-e89b-12d3-a456-426614174000', 21, 'ABIERTA'),
  ('ee000022-0022-0022-0022-000000000022', '123e4567-e89b-12d3-a456-426614174000', 22, 'ABIERTA'),
  ('ee000023-0023-0023-0023-000000000023', '123e4567-e89b-12d3-a456-426614174000', 23, 'ABIERTA')
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- 4. PEDIDO 1: Test Modificar Cantidad con Promoción 2x1
-- ========================================
-- Escenario: Pedido con 2 cervezas (2x1 activo)
-- Vamos a modificar la cantidad para probar recálculo

INSERT INTO pedidos (id, local_id, mesa_id, numero, estado, fecha_apertura) 
VALUES 
  ('ee000001-0001-0001-0001-000000000001', 
   '123e4567-e89b-12d3-a456-426614174000',
   'ee000020-0020-0020-0020-000000000020',
   201,
   'ABIERTO',
   CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 2 cervezas con promoción 2x1 aplicada
-- Subtotal: 2 × $1500 = $3000
-- Descuento: $1500 (1 gratis)
-- Total: $1500
INSERT INTO items_pedido (
  id, 
  pedido_id, 
  producto_id, 
  nombre_producto, 
  precio_unitario, 
  cantidad,
  monto_descuento,
  nombre_promocion,
  promocion_id,
  observacion
) 
VALUES 
  ('ee000101-0101-0101-0101-000000000101',
   'ee000001-0001-0001-0001-000000000001',
   'e3333333-3333-3333-3333-333333333333',
   'Cerveza Quilmes',
   1500.00,
   2,
   1500.00,
   '2x1 Cervezas',
   'e0000001-0001-0001-0001-000000000001',
   NULL)
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- 5. PEDIDO 2: Test Rotura de Combo
-- ========================================
-- Escenario: Hamburguesa + Papas con combo activo
-- Vamos a eliminar la hamburguesa y ver cómo las papas pierden el descuento

INSERT INTO pedidos (id, local_id, mesa_id, numero, estado, fecha_apertura) 
VALUES 
  ('ee000002-0002-0002-0002-000000000002', 
   '123e4567-e89b-12d3-a456-426614174000',
   'ee000021-0021-0021-0021-000000000021',
   202,
   'ABIERTO',
   CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Hamburguesa (trigger del combo)
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
  ('ee000201-0201-0201-0201-000000000201',
   'ee000002-0002-0002-0002-000000000002',
   'e1111111-1111-1111-1111-111111111111',
   'Hamburguesa Completa',
   2500.00,
   1,
   0.00,
   NULL)
ON CONFLICT (id) DO NOTHING;

-- Papas (target con 50% descuento)
INSERT INTO items_pedido (
  id, 
  pedido_id, 
  producto_id, 
  nombre_producto, 
  precio_unitario, 
  cantidad,
  monto_descuento,
  nombre_promocion,
  promocion_id,
  observacion
) 
VALUES 
  ('ee000202-0202-0202-0202-000000000202',
   'ee000002-0002-0002-0002-000000000002',
   'e2222222-2222-2222-2222-222222222222',
   'Papas Fritas',
   800.00,
   1,
   400.00,
   'Combo Hamburguesa + Papas',
   'e0000002-0002-0002-0002-000000000002',
   NULL)
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- 6. PEDIDO 3: Test con Descuento Global + Eliminar Item
-- ========================================
-- Escenario: 2 items con descuento global 10%
-- Vamos a eliminar un item y verificar que el descuento global se recalcula

INSERT INTO pedidos (id, local_id, mesa_id, numero, estado, fecha_apertura,
  desc_global_porcentaje, desc_global_razon, desc_global_usuario_id, desc_global_fecha) 
VALUES 
  ('ee000003-0003-0003-0003-000000000003', 
   '123e4567-e89b-12d3-a456-426614174000',
   'ee000022-0022-0022-0022-000000000022',
   203,
   'ABIERTO',
   CURRENT_TIMESTAMP,
   10.00,
   'Cliente frecuente',
   '550e8400-e29b-41d4-a716-446655440000',
   CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Milanesa ($3000)
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
  ('ee000301-0301-0301-0301-000000000301',
   'ee000003-0003-0003-0003-000000000003',
   'e5555555-5555-5555-5555-555555555555',
   'Milanesa Napolitana',
   3000.00,
   1,
   0.00,
   NULL),
  ('ee000302-0302-0302-0302-000000000302',
   'ee000003-0003-0003-0003-000000000003',
   'e2222222-2222-2222-2222-222222222222',
   'Papas Fritas',
   800.00,
   1,
   0.00,
   NULL)
ON CONFLICT (id) DO NOTHING;
-- Subtotal: $3800
-- Descuento global 10%: $380
-- Total: $3420

-- ========================================
-- 7. PEDIDO 4: Test Eliminar Último Item
-- ========================================
-- Escenario: Pedido con 1 solo item
-- Vamos a eliminarlo y dejar el pedido vacío

INSERT INTO pedidos (id, local_id, mesa_id, numero, estado, fecha_apertura) 
VALUES 
  ('ee000004-0004-0004-0004-000000000004', 
   '123e4567-e89b-12d3-a456-426614174000',
   'ee000023-0023-0023-0023-000000000023',
   204,
   'ABIERTO',
   CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

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
  ('ee000401-0401-0401-0401-000000000401',
   'ee000004-0004-0004-0004-000000000004',
   'e4444444-4444-4444-4444-444444444444',
   'Gaseosa Coca Cola',
   600.00,
   1,
   0.00,
   NULL)
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- RESUMEN DE DATOS CARGADOS
-- ========================================
-- 
-- PEDIDO 1 (ee000001...): 2 Cervezas con 2x1 → test modificar cantidad
-- PEDIDO 2 (ee000002...): Hamburguesa + Papas combo → test eliminar trigger
-- PEDIDO 3 (ee000003...): Milanesa + Papas con desc global → test eliminar con HU-14
-- PEDIDO 4 (ee000004...): 1 Gaseosa → test eliminar último item
