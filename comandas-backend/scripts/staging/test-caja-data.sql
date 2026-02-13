-- ============================================
-- Script de Carga de Datos para Testing Control de Caja
-- Suite: CajaController
-- Fecha: 2026-02-12
-- ============================================
-- Uso: ./ff staging scripts/staging/test-caja-data.sql

-- ========================================
-- 0. LIMPIAR DATOS EXISTENTES
-- ========================================

DELETE FROM pedidos_pagos WHERE pedido_id::text LIKE 'ca%';
DELETE FROM items_pedido WHERE pedido_id::text LIKE 'ca%';  
DELETE FROM pedidos WHERE id::text LIKE 'ca%';
DELETE FROM mesas WHERE id::text LIKE 'ca%';
DELETE FROM productos WHERE id::text LIKE 'ca%';
DELETE FROM movimientos_caja WHERE local_id = '123e4567-e89b-12d3-a456-426614174000';

-- ========================================
-- 1. PRODUCTOS PARA PEDIDOS DE PRUEBA
-- ========================================

INSERT INTO productos (id, local_id, nombre, precio, activo, color_hex, es_extra) 
VALUES 
  ('ca111111-1111-1111-1111-111111111111', '123e4567-e89b-12d3-a456-426614174000', 'Café Espresso', 800.00, true, '#6F4E37', false),
  ('ca222222-2222-2222-2222-222222222222', '123e4567-e89b-12d3-a456-426614174000', 'Medialuna', 500.00, true, '#FFD700', false),
  ('ca333333-3333-3333-3333-333333333333', '123e4567-e89b-12d3-a456-426614174000', 'Sandwich Mixto', 1200.00, true, '#FFA500', false),
  ('ca444444-4444-4444-4444-444444444444', '123e4567-e89b-12d3-a456-426614174000', 'Agua Mineral', 600.00, true, '#87CEEB', false)
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- 2. MESAS DE PRUEBA
-- ========================================

INSERT INTO mesas (id, local_id, numero, estado)
VALUES
  ('ca001000-1000-1000-1000-000000001000', '123e4567-e89b-12d3-a456-426614174000', 50, 'ABIERTA'),
  ('ca002000-2000-2000-2000-000000002000', '123e4567-e89b-12d3-a456-426614174000', 51, 'ABIERTA'),
  ('ca003000-3000-3000-3000-000000003000', '123e4567-e89b-12d3-a456-426614174000', 52, 'ABIERTA')
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- 3. PEDIDOS CERRADOS CON PAGOS MIXTOS
-- ========================================

-- Pedido 1: Mesa 50 - Venta Real (EFECTIVO)
-- Fecha: Hoy
INSERT INTO pedidos (id, local_id, mesa_id, numero, estado, fecha_apertura, fecha_cierre) 
VALUES 
  ('ca100001-0001-0001-0001-000000000001', 
   '123e4567-e89b-12d3-a456-426614174000',
   'ca001000-1000-1000-1000-000000001000',
   201,
   'CERRADO',
   CURRENT_TIMESTAMP - INTERVAL '2 hours',
   CURRENT_TIMESTAMP - INTERVAL '1 hour 30 minutes')
ON CONFLICT (id) DO NOTHING;

-- Items: 2 Cafés + 1 Medialuna = $2100
INSERT INTO items_pedido (
  id, pedido_id, producto_id, nombre_producto, 
  precio_unitario, cantidad, observacion, monto_descuento
)
VALUES 
  ('ca200001-0001-0001-0001-000000000001', 
   'ca100001-0001-0001-0001-000000000001',
   'ca111111-1111-1111-1111-111111111111',
   'Café Espresso', 800.00, 2, '', 0.00),
  ('ca200002-0002-0002-0002-000000000002', 
   'ca100001-0001-0001-0001-000000000001',
   'ca222222-2222-2222-2222-222222222222',
   'Medialuna', 500.00, 1, '', 0.00)
ON CONFLICT (id) DO NOTHING;

-- Pago en EFECTIVO
INSERT INTO pedidos_pagos (id, pedido_id, medio_pago, monto, fecha)
VALUES 
  ('ca300001-0001-0001-0001-000000000001',
   'ca100001-0001-0001-0001-000000000001',
   'EFECTIVO', 2100.00, CURRENT_TIMESTAMP - INTERVAL '1 hour 30 minutes')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------

-- Pedido 2: Mesa 51 - Venta Real (TARJETA)
INSERT INTO pedidos (id, local_id, mesa_id, numero, estado, fecha_apertura, fecha_cierre) 
VALUES 
  ('ca100002-0002-0002-0002-000000000002', 
   '123e4567-e89b-12d3-a456-426614174000',
   'ca002000-2000-2000-2000-000000002000',
   202,
   'CERRADO',
   CURRENT_TIMESTAMP - INTERVAL '1 hour',
   CURRENT_TIMESTAMP - INTERVAL '30 minutes')
ON CONFLICT (id) DO NOTHING;

-- Items: 1 Sandwich + 1 Agua = $1800
INSERT INTO items_pedido (
  id, pedido_id, producto_id, nombre_producto, 
  precio_unitario, cantidad, observacion, monto_descuento
)
VALUES 
  ('ca200003-0003-0003-0003-000000000003', 
   'ca100002-0002-0002-0002-000000000002',
   'ca333333-3333-3333-3333-333333333333',
   'Sandwich Mixto', 1200.00, 1, 'Sin tomate', 0.00),
  ('ca200004-0004-0004-0004-000000000004', 
   'ca100002-0002-0002-0002-000000000002',
   'ca444444-4444-4444-4444-444444444444',
   'Agua Mineral', 600.00, 1, '', 0.00)
ON CONFLICT (id) DO NOTHING;

-- Pago con TARJETA
INSERT INTO pedidos_pagos (id, pedido_id, medio_pago, monto, fecha)
VALUES 
  ('ca300002-0002-0002-0002-000000000002',
   'ca100002-0002-0002-0002-000000000002',
   'TARJETA', 1800.00, CURRENT_TIMESTAMP - INTERVAL '30 minutes')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------

-- Pedido 3: Mesa 52 - Consumo Interno (A_CUENTA)
INSERT INTO pedidos (id, local_id, mesa_id, numero, estado, fecha_apertura, fecha_cierre) 
VALUES 
  ('ca100003-0003-0003-0003-000000000003', 
   '123e4567-e89b-12d3-a456-426614174000',
   'ca003000-3000-3000-3000-000000003000',
   203,
   'CERRADO',
   CURRENT_TIMESTAMP - INTERVAL '45 minutes',
   CURRENT_TIMESTAMP - INTERVAL '15 minutes')
ON CONFLICT (id) DO NOTHING;

-- Items: 3 Cafés = $2400
INSERT INTO items_pedido (
  id, pedido_id, producto_id, nombre_producto, 
  precio_unitario, cantidad, observacion, monto_descuento
)
VALUES 
  ('ca200005-0005-0005-0005-000000000005', 
   'ca100003-0003-0003-0003-000000000003',
   'ca111111-1111-1111-1111-111111111111',
   'Café Espresso', 800.00, 3, 'Para el staff', 0.00)
ON CONFLICT (id) DO NOTHING;

-- Pago A_CUENTA (consumo interno)
INSERT INTO pedidos_pagos (id, pedido_id, medio_pago, monto, fecha)
VALUES 
  ('ca300003-0003-0003-0003-000000000003',
   'ca100003-0003-0003-0003-000000000003',
   'A_CUENTA', 2400.00, CURRENT_TIMESTAMP - INTERVAL '15 minutes')
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- 4. PEDIDO ABIERTO (NO DEBE AFECTAR REPORTE)
-- ========================================

INSERT INTO pedidos (id, local_id, mesa_id, numero, estado, fecha_apertura) 
VALUES 
  ('ca100099-0099-0099-0099-000000000099', 
   '123e4567-e89b-12d3-a456-426614174000',
   'ca001000-1000-1000-1000-000000001000',
   299,
   'ABIERTO',
   CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO items_pedido (
  id, pedido_id, producto_id, nombre_producto, 
  precio_unitario, cantidad, observacion, monto_descuento
)
VALUES 
  ('ca200099-0099-0099-0099-000000000099', 
   'ca100099-0099-0099-0099-000000000099',
   'ca111111-1111-1111-1111-111111111111',
   'Café Espresso', 800.00, 1, '', 0.00)
ON CONFLICT (id) DO NOTHING;

-- ========================================
-- RESUMEN ESPERADO PARA REPORTE DE HOY:
-- ========================================
-- Ventas Reales:
--   - Pedido 201 (EFECTIVO): $2100
--   - Pedido 202 (TARJETA):  $1800
--   TOTAL VENTAS REALES: $3900
--
-- Consumo Interno:
--   - Pedido 203 (A_CUENTA): $2400
--   TOTAL CONSUMO INTERNO: $2400
--
-- Desglose por Medio de Pago:
--   - EFECTIVO: $2100
--   - TARJETA: $1800
--   - A_CUENTA: $2400
--
-- Balance Inicial Efectivo: $2100 (del pago en efectivo)
-- Sin egresos: Balance Final = $2100
