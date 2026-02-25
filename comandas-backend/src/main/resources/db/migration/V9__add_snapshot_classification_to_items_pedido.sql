-- V9: Agregar campos de snapshot de clasificación al ItemPedido
-- Estos campos capturan el estado del producto al momento de la venta,
-- garantizando inmutabilidad histórica (no dependen del catálogo vivo).

ALTER TABLE items_pedido ADD COLUMN grupo_variante_id_snapshot UUID;
ALTER TABLE items_pedido ADD COLUMN cantidad_discos_snapshot INTEGER;
ALTER TABLE items_pedido ADD COLUMN categoria_id_snapshot UUID;

-- Estos campos son nullable porque:
-- 1. Items históricos no tenían esta información
-- 2. Productos sin grupo de variantes o sin categoría generan nulls legítimos
