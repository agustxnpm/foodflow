-- Script de carga de datos de prueba para Productos
-- Fecha: 2026-02-05
-- Propósito: Poblar la tabla productos con datos de ejemplo para testing del CRUD

-- Limpiar datos existentes (solo productos, no afecta otros datos)
DELETE FROM items_pedido WHERE producto_id IN (SELECT id FROM productos WHERE local_id = '123e4567-e89b-12d3-a456-426614174000');
DELETE FROM productos WHERE local_id = '123e4567-e89b-12d3-a456-426614174000';

-- Productos con diferentes colores para testing de filtrado visual
-- Local ID: 123e4567-e89b-12d3-a456-426614174000 (mismo usado en el controller)

-- PRODUCTOS ROJOS (#FF0000 y variantes)
INSERT INTO productos (id, local_id, nombre, precio, activo, color_hex) VALUES
('11111111-1111-1111-1111-111111111111', '123e4567-e89b-12d3-a456-426614174000', 'Hamburguesa Clásica', 1500.00, true, '#FF0000'),
('11111111-1111-1111-1111-111111111112', '123e4567-e89b-12d3-a456-426614174000', 'Milanesa Napolitana', 1800.00, true, '#FF0000'),
('11111111-1111-1111-1111-111111111113', '123e4567-e89b-12d3-a456-426614174000', 'Pizza Muzzarella', 2000.00, true, '#FF5733');

-- PRODUCTOS VERDES (#00FF00 y variantes)
INSERT INTO productos (id, local_id, nombre, precio, activo, color_hex) VALUES
('22222222-2222-2222-2222-222222222221', '123e4567-e89b-12d3-a456-426614174000', 'Ensalada César', 1200.00, true, '#00FF00'),
('22222222-2222-2222-2222-222222222222', '123e4567-e89b-12d3-a456-426614174000', 'Ensalada Mixta', 900.00, true, '#00FF00'),
('22222222-2222-2222-2222-222222222223', '123e4567-e89b-12d3-a456-426614174000', 'Wrap Vegetariano', 1300.00, true, '#32CD32');

-- PRODUCTOS AZULES (#0000FF y variantes)
INSERT INTO productos (id, local_id, nombre, precio, activo, color_hex) VALUES
('33333333-3333-3333-3333-333333333331', '123e4567-e89b-12d3-a456-426614174000', 'Cerveza Artesanal', 800.00, true, '#0000FF'),
('33333333-3333-3333-3333-333333333332', '123e4567-e89b-12d3-a456-426614174000', 'Agua Mineral', 300.00, true, '#0000FF'),
('33333333-3333-3333-3333-333333333333', '123e4567-e89b-12d3-a456-426614174000', 'Gaseosa', 400.00, true, '#1E90FF');

-- PRODUCTOS AMARILLOS (#FFFF00 y variantes)
INSERT INTO productos (id, local_id, nombre, precio, activo, color_hex) VALUES
('44444444-4444-4444-4444-444444444441', '123e4567-e89b-12d3-a456-426614174000', 'Papas Fritas', 700.00, true, '#FFFF00'),
('44444444-4444-4444-4444-444444444442', '123e4567-e89b-12d3-a456-426614174000', 'Empanadas (x6)', 1000.00, true, '#FFD700');

-- PRODUCTOS NARANJAS (postres/café)
INSERT INTO productos (id, local_id, nombre, precio, activo, color_hex) VALUES
('55555555-5555-5555-5555-555555555551', '123e4567-e89b-12d3-a456-426614174000', 'Café Espresso', 350.00, true, '#FF8C00'),
('55555555-5555-5555-5555-555555555552', '123e4567-e89b-12d3-a456-426614174000', 'Tiramisu', 950.00, true, '#FFA500');

-- PRODUCTOS BLANCOS (lácteos)
INSERT INTO productos (id, local_id, nombre, precio, activo, color_hex) VALUES
('66666666-6666-6666-6666-666666666661', '123e4567-e89b-12d3-a456-426614174000', 'Flan Casero', 600.00, true, '#FFFFFF'),
('66666666-6666-6666-6666-666666666662', '123e4567-e89b-12d3-a456-426614174000', 'Helado (1 bocha)', 450.00, true, '#F5F5F5');

-- PRODUCTO INACTIVO (para testing de edición de estado)
INSERT INTO productos (id, local_id, nombre, precio, activo, color_hex) VALUES
('99999999-9999-9999-9999-999999999999', '123e4567-e89b-12d3-a456-426614174000', 'Producto Descontinuado', 100.00, false, '#808080');

-- Verificación
SELECT 
    COUNT(*) as total_productos,
    COUNT(CASE WHEN activo = true THEN 1 END) as activos,
    COUNT(CASE WHEN activo = false THEN 1 END) as inactivos
FROM productos 
WHERE local_id = '123e4567-e89b-12d3-a456-426614174000';

SELECT 
    color_hex,
    COUNT(*) as cantidad
FROM productos 
WHERE local_id = '123e4567-e89b-12d3-a456-426614174000'
GROUP BY color_hex
ORDER BY color_hex;
