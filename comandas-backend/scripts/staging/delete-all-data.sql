-- ============================================================
-- delete-all-data.sql
-- Limpia TODOS los datos de todas las tablas del sistema.
--
-- Uso: Preparar la base de datos para carga real del menú.
--
-- ⚠️  DESTRUCTIVO — Borra toda la información existente.
--     No afecta el schema ni el historial de Flyway.
--
-- Orden: hijos antes que padres (respetar foreign keys).
-- ============================================================

BEGIN;

-- Tablas hijas (sin dependencias entrantes)
TRUNCATE TABLE items_pedido_extras        CASCADE;
TRUNCATE TABLE items_pedido               CASCADE;
TRUNCATE TABLE pedidos_pagos              CASCADE;
TRUNCATE TABLE pedidos                    CASCADE;

TRUNCATE TABLE promocion_productos_scope  CASCADE;
TRUNCATE TABLE promociones                CASCADE;

TRUNCATE TABLE movimientos_stock          CASCADE;
TRUNCATE TABLE movimientos_caja           CASCADE;
TRUNCATE TABLE jornadas_caja              CASCADE;

TRUNCATE TABLE categorias                 CASCADE;
TRUNCATE TABLE productos                  CASCADE;
TRUNCATE TABLE mesas                      CASCADE;
TRUNCATE TABLE locales                    CASCADE;

COMMIT;

-- Verificación rápida (debería dar 0 en todas)
SELECT 'locales'                  AS tabla, COUNT(*) FROM locales
UNION ALL SELECT 'mesas',                   COUNT(*) FROM mesas
UNION ALL SELECT 'productos',               COUNT(*) FROM productos
UNION ALL SELECT 'categorias',              COUNT(*) FROM categorias
UNION ALL SELECT 'pedidos',                 COUNT(*) FROM pedidos
UNION ALL SELECT 'items_pedido',            COUNT(*) FROM items_pedido
UNION ALL SELECT 'items_pedido_extras',     COUNT(*) FROM items_pedido_extras
UNION ALL SELECT 'pedidos_pagos',           COUNT(*) FROM pedidos_pagos
UNION ALL SELECT 'promociones',             COUNT(*) FROM promociones
UNION ALL SELECT 'promocion_productos_scope', COUNT(*) FROM promocion_productos_scope
UNION ALL SELECT 'movimientos_stock',       COUNT(*) FROM movimientos_stock
UNION ALL SELECT 'movimientos_caja',        COUNT(*) FROM movimientos_caja
UNION ALL SELECT 'jornadas_caja',           COUNT(*) FROM jornadas_caja
ORDER BY tabla;
