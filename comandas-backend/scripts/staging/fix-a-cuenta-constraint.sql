-- Actualizar constraint de pedidos_pagos para soportar A_CUENTA
-- y limpiar datos de prueba antiguos

-- 1. Limpiar datos de prueba antiguos de caja
DELETE FROM pedidos_pagos WHERE pedido_id LIKE 'ca%';
DELETE FROM items_pedido WHERE pedido_id LIKE 'ca%';  
DELETE FROM pedidos WHERE id LIKE 'ca%';
DELETE FROM mesas WHERE id LIKE 'ca%';
DELETE FROM productos WHERE id LIKE 'ca%';
DELETE FROM movimientos_caja;

-- 2. Eliminar constraint viejo
ALTER TABLE pedidos_pagos DROP CONSTRAINT IF EXISTS pedidos_pagos_medio_pago_check;

-- 3. Agregar constraint nuevo con A_CUENTA
ALTER TABLE pedidos_pagos 
ADD CONSTRAINT pedidos_pagos_medio_pago_check 
CHECK (medio_pago IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA', 'QR', 'A_CUENTA'));
