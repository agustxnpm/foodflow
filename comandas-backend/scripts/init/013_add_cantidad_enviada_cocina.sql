-- HU-29: Agregar campo para tracking de cantidad enviada a cocina por ítem.
-- Permite calcular el delta (unidades nuevas) cuando se reenvía una comanda
-- tras agregar más unidades del mismo producto.
-- Default 0: el ítem nunca fue enviado a cocina.
ALTER TABLE items_pedido ADD COLUMN cantidad_enviada_cocina INTEGER NOT NULL DEFAULT 0;
