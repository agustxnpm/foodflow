-- Script de carga de datos de staging para FoodFlow
-- Uso: ./ff staging scripts/staging/seed-example.sql

-- Insertar local de prueba "Meiser Sandwicheria"
-- Usa el mismo UUID que está hardcodeado en MesaController.java
INSERT INTO locales (id, nombre) 
VALUES ('123e4567-e89b-12d3-a456-426614174000', 'Meiser Sandwicheria')
ON CONFLICT (id) DO NOTHING;

-- Insertar mesas de prueba para el local
-- Estados: LIBRE (disponible para asignar) y ABIERTA (con pedido en curso)
INSERT INTO mesas (id, local_id, numero, estado) 
VALUES 
  -- Mesas libres
  ('a1111111-1111-1111-1111-111111111111', '123e4567-e89b-12d3-a456-426614174000', 1, 'LIBRE'),
  ('a2222222-2222-2222-2222-222222222222', '123e4567-e89b-12d3-a456-426614174000', 2, 'LIBRE'),
  ('a3333333-3333-3333-3333-333333333333', '123e4567-e89b-12d3-a456-426614174000', 3, 'LIBRE'),
  ('a4444444-4444-4444-4444-444444444444', '123e4567-e89b-12d3-a456-426614174000', 4, 'LIBRE'),
  
  -- Mesas con pedidos abiertos
  ('a5555555-5555-5555-5555-555555555555', '123e4567-e89b-12d3-a456-426614174000', 5, 'ABIERTA'),
  ('a6666666-6666-6666-6666-666666666666', '123e4567-e89b-12d3-a456-426614174000', 6, 'ABIERTA'),
  ('a7777777-7777-7777-7777-777777777777', '123e4567-e89b-12d3-a456-426614174000', 7, 'ABIERTA')
ON CONFLICT (id) DO NOTHING;

SELECT 
  'Datos de staging cargados:' as mensaje,
  (SELECT COUNT(*) FROM locales WHERE id = '123e4567-e89b-12d3-a456-426614174000') as locales_insertados,
  (SELECT COUNT(*) FROM mesas WHERE local_id = '123e4567-e89b-12d3-a456-426614174000') as mesas_insertadas;
