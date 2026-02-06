-- Script de carga de datos de prueba para Promociones
-- Fecha: 2026-02-05
-- Propósito: Poblar la tabla promociones con datos de ejemplo para testing del CRUD

-- Limpiar datos existentes de promociones
DELETE FROM promociones WHERE local_id = '123e4567-e89b-12d3-a456-426614174000';

-- Local ID: 123e4567-e89b-12d3-a456-426614174000 (mismo usado en el controller)

-- ============================================
-- Promoción 1: 2x1 en Cervezas - Solo Viernes y Sábados por la noche
-- Estrategia: CANTIDAD_FIJA (2 llevas, 1 pagas)
-- Trigger: TEMPORAL (Febrero 2026, Vie-Sáb, 18:00-23:59)
-- ============================================
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
) VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    '123e4567-e89b-12d3-a456-426614174000',
    '2x1 en Cervezas',
    'Llevá 2 cervezas artesanales, pagá 1. Solo fines de semana por la noche',
    10,
    'ACTIVA',
    'CANTIDAD_FIJA',
    2,
    1,
    '[
        {
            "tipo": "TEMPORAL",
            "fechaDesde": "2026-02-01",
            "fechaHasta": "2026-02-28",
            "diasSemana": ["FRIDAY", "SATURDAY"],
            "horaDesde": "18:00",
            "horaHasta": "23:59"
        }
    ]'
);

-- ============================================
-- Promoción 2: 20% OFF en Pizzas - Todo el mes
-- Estrategia: DESCUENTO_DIRECTO (PORCENTAJE, 20%)
-- Trigger: TEMPORAL (Febrero completo, todos los días)
-- ============================================
INSERT INTO promociones (
    id, 
    local_id, 
    nombre, 
    descripcion, 
    prioridad, 
    estado, 
    tipo_estrategia,
    modo_descuento,
    valor_descuento,
    triggers_json
) VALUES (
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    '123e4567-e89b-12d3-a456-426614174000',
    '20% OFF en Pizzas',
    'Descuento del 20% en todas las pizzas durante febrero',
    5,
    'ACTIVA',
    'DESCUENTO_DIRECTO',
    'PORCENTAJE',
    20.00,
    '[
        {
            "tipo": "TEMPORAL",
            "fechaDesde": "2026-02-01",
            "fechaHasta": "2026-02-28"
        }
    ]'
);

-- ============================================
-- Promoción 3: Happy Hour Hamburguesas
-- Estrategia: DESCUENTO_DIRECTO (MONTO_FIJO, $500)
-- Trigger: TEMPORAL (Febrero, Lun-Vie, 17:00-19:00) + CONTENIDO (Hamburguesas)
-- ============================================
INSERT INTO promociones (
    id, 
    local_id, 
    nombre, 
    descripcion, 
    prioridad, 
    estado, 
    tipo_estrategia,
    modo_descuento,
    valor_descuento,
    triggers_json
) VALUES (
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    '123e4567-e89b-12d3-a456-426614174000',
    'Happy Hour Hamburguesas',
    '$500 de descuento en hamburguesas durante happy hour',
    15,
    'ACTIVA',
    'DESCUENTO_DIRECTO',
    'MONTO_FIJO',
    500.00,
    '[
        {
            "tipo": "TEMPORAL",
            "fechaDesde": "2026-02-01",
            "fechaHasta": "2026-02-28",
            "diasSemana": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
            "horaDesde": "17:00",
            "horaHasta": "19:00"
        },
        {
            "tipo": "CONTENIDO",
            "productosRequeridos": ["11111111-1111-1111-1111-111111111111"]
        }
    ]'
);

-- ============================================
-- Promoción 4: Combo Ensaladas (3x2)
-- Estrategia: COMBO_CONDICIONAL (3 trigger, 33% beneficio)
-- Trigger: CONTENIDO (Ensaladas)
-- ============================================
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
) VALUES (
    'dddddddd-dddd-dddd-dddd-dddddddddddd',
    '123e4567-e89b-12d3-a456-426614174000',
    'Combo Ensaladas 3x2',
    'Comprando 3 ensaladas, la más económica sale gratis',
    8,
    'ACTIVA',
    'COMBO_CONDICIONAL',
    3,
    33.33,
    '[
        {
            "tipo": "CONTENIDO",
            "productosRequeridos": [
                "22222222-2222-2222-2222-222222222221",
                "22222222-2222-2222-2222-222222222222",
                "22222222-2222-2222-2222-222222222223"
            ]
        }
    ]'
);

-- ============================================
-- Promoción 5: Compra Mínima $5000 - 10% OFF
-- Estrategia: DESCUENTO_DIRECTO (PORCENTAJE, 10%)
-- Trigger: MONTO_MINIMO ($5000)
-- ============================================
INSERT INTO promociones (
    id, 
    local_id, 
    nombre, 
    descripcion, 
    prioridad, 
    estado, 
    tipo_estrategia,
    modo_descuento,
    valor_descuento,
    triggers_json
) VALUES (
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
    '123e4567-e89b-12d3-a456-426614174000',
    'Compra Mínima $5000',
    '10% de descuento en compras superiores a $5000',
    3,
    'ACTIVA',
    'DESCUENTO_DIRECTO',
    'PORCENTAJE',
    10.00,
    '[
        {
            "tipo": "MONTO_MINIMO",
            "montoMinimo": 5000
        }
    ]'
);

-- ============================================
-- Promoción 6: Promo Vencida (INACTIVA)
-- Para testear filtrado por estado
-- ============================================
INSERT INTO promociones (
    id, 
    local_id, 
    nombre, 
    descripcion, 
    prioridad, 
    estado, 
    tipo_estrategia,
    modo_descuento,
    valor_descuento,
    triggers_json
) VALUES (
    'ffffffff-ffff-ffff-ffff-ffffffffffff',
    '123e4567-e89b-12d3-a456-426614174000',
    'Promo Enero Vencida',
    'Promoción que ya finalizó',
    5,
    'INACTIVA',
    'DESCUENTO_DIRECTO',
    'PORCENTAJE',
    15.00,
    '[
        {
            "tipo": "TEMPORAL",
            "fechaDesde": "2026-01-01",
            "fechaHasta": "2026-01-31"
        }
    ]'
);

-- Verificar carga
SELECT 
    nombre, 
    estado, 
    tipo_estrategia,
    prioridad,
    jsonb_array_length(triggers_json::jsonb) as cantidad_triggers
FROM promociones 
WHERE local_id = '123e4567-e89b-12d3-a456-426614174000'
ORDER BY prioridad DESC;
