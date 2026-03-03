-- ============================================================
-- HU-29: Timestamps para gestión de envío a cocina y reimpresiones
-- ============================================================

-- Timestamp del último envío de comanda a cocina en el pedido.
-- Se usa para determinar cuáles ítems son "nuevos" vs "ya enviados".
ALTER TABLE pedidos
    ADD COLUMN IF NOT EXISTS ultimo_envio_cocina TIMESTAMP;

-- Timestamp de creación individual de cada ítem.
-- Se compara contra ultimo_envio_cocina del pedido para calcular esNuevo.
ALTER TABLE items_pedido
    ADD COLUMN IF NOT EXISTS fecha_agregado TIMESTAMP;

-- Backfill: Los ítems legacy sin fecha_agregado se dejan como NULL.
-- El dominio los tratará como "nuevos" (comportamiento safe por defecto).
