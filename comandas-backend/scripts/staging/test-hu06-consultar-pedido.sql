-- Script de datos de prueba para HU-06: Consultar Detalle de Pedido
-- Escenarios cubiertos:
-- 1. Mesa con pedido activo con varios ítems
-- 2. Mesa libre (sin pedido activo)
-- 3. Mesa con pedido vacío (recién abierta)

-- Limpiar datos previos del local de prueba
DELETE FROM items_pedido WHERE pedido_id IN (
    SELECT id FROM pedidos WHERE local_id = '123e4567-e89b-12d3-a456-426614174000'
);
DELETE FROM pedidos WHERE local_id = '123e4567-e89b-12d3-a456-426614174000';
DELETE FROM mesas WHERE local_id = '123e4567-e89b-12d3-a456-426614174000';
DELETE FROM productos WHERE local_id = '123e4567-e89b-12d3-a456-426614174000';

-- 1. Crear productos del catálogo
INSERT INTO productos (id, activo, local_id, nombre, precio) VALUES
    ('a1111111-1111-1111-1111-111111111111', true, '123e4567-e89b-12d3-a456-426614174000', 'Pizza Napolitana', 350.00),
    ('a2222222-2222-2222-2222-222222222222', true, '123e4567-e89b-12d3-a456-426614174000', 'Coca Cola 1.5L', 120.50),
    ('a3333333-3333-3333-3333-333333333333', true, '123e4567-e89b-12d3-a456-426614174000', 'Empanadas de Carne', 45.00),
    ('a4444444-4444-4444-4444-444444444444', true, '123e4567-e89b-12d3-a456-426614174000', 'Cerveza Quilmes', 180.00),
    ('a5555555-5555-5555-5555-555555555555', true, '123e4567-e89b-12d3-a456-426614174000', 'Fernet con Cola', 250.00);

-- 2. Crear mesas con diferentes estados
INSERT INTO mesas (id, local_id, numero, estado) VALUES
    -- Mesa 10: ABIERTA con pedido completo
    ('b1111111-1111-1111-1111-111111111111', '123e4567-e89b-12d3-a456-426614174000', 10, 'ABIERTA'),
    -- Mesa 5: LIBRE (sin pedido)
    ('b2222222-2222-2222-2222-222222222222', '123e4567-e89b-12d3-a456-426614174000', 5, 'LIBRE'),
    -- Mesa 8: ABIERTA pero pedido vacío
    ('b3333333-3333-3333-3333-333333333333', '123e4567-e89b-12d3-a456-426614174000', 8, 'ABIERTA'),
    -- Mesa 15: ABIERTA con un solo ítem
    ('b4444444-4444-4444-4444-444444444444', '123e4567-e89b-12d3-a456-426614174000', 15, 'ABIERTA');

-- 3. Crear pedidos
INSERT INTO pedidos (id, local_id, mesa_id, numero, estado, fecha_apertura) VALUES
    -- Pedido de mesa 10 (completo)
    ('c1111111-1111-1111-1111-111111111111', '123e4567-e89b-12d3-a456-426614174000', 
     'b1111111-1111-1111-1111-111111111111', 42, 'ABIERTO', NOW() - INTERVAL '2 hours'),
    -- Pedido de mesa 8 (vacío)
    ('c2222222-2222-2222-2222-222222222222', '123e4567-e89b-12d3-a456-426614174000', 
     'b3333333-3333-3333-3333-333333333333', 43, 'ABIERTO', NOW() - INTERVAL '30 minutes'),
    -- Pedido de mesa 15 (un ítem)
    ('c3333333-3333-3333-3333-333333333333', '123e4567-e89b-12d3-a456-426614174000', 
     'b4444444-4444-4444-4444-444444444444', 44, 'ABIERTO', NOW() - INTERVAL '1 hour');

-- 4. Crear ítems del pedido de mesa 10 (múltiples ítems)
INSERT INTO items_pedido (id, pedido_id, producto_id, nombre_producto, cantidad, precio_unitario, observacion) VALUES
    -- Item 1: 2 Pizzas
    ('d1111111-1111-1111-1111-111111111111', 'c1111111-1111-1111-1111-111111111111',
     'a1111111-1111-1111-1111-111111111111', 'Pizza Napolitana', 2, 350.00, 'Sin aceitunas'),
    -- Item 2: 3 Coca Colas
    ('d2222222-2222-2222-2222-222222222222', 'c1111111-1111-1111-1111-111111111111',
     'a2222222-2222-2222-2222-222222222222', 'Coca Cola 1.5L', 3, 120.50, NULL),
    -- Item 3: 6 Empanadas
    ('d3333333-3333-3333-3333-333333333333', 'c1111111-1111-1111-1111-111111111111',
     'a3333333-3333-3333-3333-333333333333', 'Empanadas de Carne', 6, 45.00, 'Bien cocidas'),
    -- Item 4: 2 Cervezas
    ('d4444444-4444-4444-4444-444444444444', 'c1111111-1111-1111-1111-111111111111',
     'a4444444-4444-4444-4444-444444444444', 'Cerveza Quilmes', 2, 180.00, 'Bien frías');

-- 5. Crear ítem del pedido de mesa 15 (un solo ítem)
INSERT INTO items_pedido (id, pedido_id, producto_id, nombre_producto, cantidad, precio_unitario, observacion) VALUES
    ('d5555555-5555-5555-5555-555555555555', 'c3333333-3333-3333-3333-333333333333',
     'a5555555-5555-5555-5555-555555555555', 'Fernet con Cola', 1, 250.00, NULL);

-- Verificación de datos insertados
SELECT 
    '=== RESUMEN DE DATOS INSERTADOS ===' as info;

SELECT 
    'Productos' as tipo,
    COUNT(*) as cantidad 
FROM productos 
WHERE local_id = '123e4567-e89b-12d3-a456-426614174000';

SELECT 
    'Mesas' as tipo,
    COUNT(*) as total,
    SUM(CASE WHEN estado = 'ABIERTA' THEN 1 ELSE 0 END) as abiertas,
    SUM(CASE WHEN estado = 'LIBRE' THEN 1 ELSE 0 END) as libres
FROM mesas 
WHERE local_id = '123e4567-e89b-12d3-a456-426614174000';

SELECT 
    'Pedidos' as tipo,
    COUNT(*) as cantidad 
FROM pedidos 
WHERE local_id = '123e4567-e89b-12d3-a456-426614174000';

SELECT 
    'Items' as tipo,
    COUNT(*) as cantidad 
FROM items_pedido 
WHERE pedido_id IN (
    SELECT id FROM pedidos WHERE local_id = '123e4567-e89b-12d3-a456-426614174000'
);

-- Detalle por mesa
SELECT 
    m.numero as mesa,
    m.estado as estado_mesa,
    COALESCE(p.numero, 0) as num_pedido,
    COALESCE(p.estado::text, 'N/A') as estado_pedido,
    COUNT(i.id) as items
FROM mesas m
LEFT JOIN pedidos p ON m.id = p.mesa_id AND p.estado = 'ABIERTO'
LEFT JOIN items_pedido i ON p.id = i.pedido_id
WHERE m.local_id = '123e4567-e89b-12d3-a456-426614174000'
GROUP BY m.numero, m.estado, p.numero, p.estado
ORDER BY m.numero;
