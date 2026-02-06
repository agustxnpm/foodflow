#!/bin/bash

# ============================================
# Suite de Tests CURL para PromocionController
# Archivo: test-promociones-crud.sh
# Fecha: 2026-02-05
# ============================================

BASE_URL="http://localhost:8080/api/promociones"
CONTENT_TYPE="Content-Type: application/json"

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# UUIDs de promociones de prueba (del seed)
PROMO_2X1_ID="aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
PROMO_20OFF_ID="bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
PROMO_HAPPY_ID="cccccccc-cccc-cccc-cccc-cccccccccccc"
PROMO_COMBO_ID="dddddddd-dddd-dddd-dddd-dddddddddddd"
PROMO_MINIMA_ID="eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"
PROMO_INACTIVA_ID="ffffffff-ffff-ffff-ffff-ffffffffffff"

# Producto hamburguesa para triggers
PRODUCTO_HAMBURGUESA="11111111-1111-1111-1111-111111111111"

# Función para imprimir headers de secciones
print_section() {
    echo ""
    echo -e "${BLUE}============================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}============================================${NC}"
    echo ""
}

# Función para imprimir resultado
print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ $2${NC}"
    else
        echo -e "${RED}✗ $2${NC}"
    fi
}

# Esperar un momento entre requests
wait_request() {
    sleep 0.5
}

# ============================================
# TEST 1: GET /api/promociones
# Listar todas las promociones
# ============================================
print_section "TEST 1: Listar todas las promociones"

echo "Request: GET $BASE_URL"
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL")
HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | head -n -1)

echo "Response Code: $HTTP_CODE"
echo "Response Body:"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

if [ "$HTTP_CODE" -eq 200 ]; then
    COUNT=$(echo "$BODY" | jq 'length' 2>/dev/null)
    print_result 0 "Listado exitoso. Total promociones: $COUNT"
else
    print_result 1 "Error al listar promociones"
fi

wait_request

# ============================================
# TEST 2: GET /api/promociones?estado=ACTIVA
# Filtrar promociones activas
# ============================================
print_section "TEST 2: Filtrar promociones ACTIVAS"

echo "Request: GET $BASE_URL?estado=ACTIVA"
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL?estado=ACTIVA")
HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | head -n -1)

echo "Response Code: $HTTP_CODE"
echo "Response Body:"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

if [ "$HTTP_CODE" -eq 200 ]; then
    COUNT=$(echo "$BODY" | jq 'length' 2>/dev/null)
    print_result 0 "Filtrado exitoso. Promociones activas: $COUNT"
else
    print_result 1 "Error al filtrar promociones"
fi

wait_request

# ============================================
# TEST 3: GET /api/promociones?estado=INACTIVA
# Filtrar promociones inactivas
# ============================================
print_section "TEST 3: Filtrar promociones INACTIVAS"

echo "Request: GET $BASE_URL?estado=INACTIVA"
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL?estado=INACTIVA")
HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | head -n -1)

echo "Response Code: $HTTP_CODE"
echo "Response Body:"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

if [ "$HTTP_CODE" -eq 200 ]; then
    COUNT=$(echo "$BODY" | jq 'length' 2>/dev/null)
    print_result 0 "Filtrado exitoso. Promociones inactivas: $COUNT"
else
    print_result 1 "Error al filtrar promociones"
fi

wait_request

# ============================================
# TEST 4: GET /api/promociones/{id}
# Obtener detalle de una promoción específica
# ============================================
print_section "TEST 4: Obtener detalle de promoción 2x1 Cervezas"

echo "Request: GET $BASE_URL/$PROMO_2X1_ID"
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/$PROMO_2X1_ID")
HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | head -n -1)

echo "Response Code: $HTTP_CODE"
echo "Response Body:"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

if [ "$HTTP_CODE" -eq 200 ]; then
    NOMBRE=$(echo "$BODY" | jq -r '.nombre' 2>/dev/null)
    print_result 0 "Detalle obtenido: $NOMBRE"
else
    print_result 1 "Error al obtener detalle"
fi

wait_request

# ============================================
# TEST 5: POST /api/promociones
# Crear nueva promoción con múltiples triggers
# ============================================
print_section "TEST 5: Crear nueva promoción - Miércoles de Descuento"

NUEVO_PROMO_JSON='{
    "nombre": "Miércoles de Descuento Total",
    "descripcion": "15% en toda la carta los miércoles",
    "prioridad": 12,
    "tipoEstrategia": "DESCUENTO_DIRECTO",
    "descuentoDirecto": {
        "modo": "PORCENTAJE",
        "valor": 15
    },
    "triggers": [
        {
            "tipo": "TEMPORAL",
            "fechaDesde": "2026-02-01",
            "fechaHasta": "2026-02-28",
            "diasSemana": ["WEDNESDAY"]
        }
    ]
}'

echo "Request: POST $BASE_URL"
echo "Body:"
echo "$NUEVO_PROMO_JSON" | jq '.'
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL" \
    -H "$CONTENT_TYPE" \
    -d "$NUEVO_PROMO_JSON")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | head -n -1)

echo "Response Code: $HTTP_CODE"
echo "Response Body:"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

if [ "$HTTP_CODE" -eq 201 ]; then
    CREATED_ID=$(echo "$BODY" | jq -r '.id' 2>/dev/null)
    print_result 0 "Promoción creada exitosamente. ID: $CREATED_ID"
    # Guardar ID para usar en tests posteriores
    NUEVA_PROMO_ID="$CREATED_ID"
else
    print_result 1 "Error al crear promoción"
fi

wait_request

# ============================================
# TEST 6: POST /api/promociones
# Crear promoción con trigger de CONTENIDO
# ============================================
print_section "TEST 6: Crear promoción con trigger de CONTENIDO"

PROMO_CONTENIDO_JSON='{
    "nombre": "Pack Familiar Hamburguesas",
    "descripcion": "4 hamburguesas por el precio de 3",
    "prioridad": 7,
    "tipoEstrategia": "CANTIDAD_FIJA",
    "cantidadFija": {
        "cantidadLlevas": 4,
        "cantidadPagas": 3
    },
    "triggers": [
        {
            "tipo": "CONTENIDO",
            "productosRequeridos": ["'$PRODUCTO_HAMBURGUESA'"]
        }
    ]
}'

echo "Request: POST $BASE_URL"
echo "Body:"
echo "$PROMO_CONTENIDO_JSON" | jq '.'
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL" \
    -H "$CONTENT_TYPE" \
    -d "$PROMO_CONTENIDO_JSON")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | head -n -1)

echo "Response Code: $HTTP_CODE"
echo "Response Body:"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

if [ "$HTTP_CODE" -eq 201 ]; then
    CREATED_ID=$(echo "$BODY" | jq -r '.id' 2>/dev/null)
    print_result 0 "Promoción con CONTENIDO creada. ID: $CREATED_ID"
    PROMO_CONTENIDO_ID="$CREATED_ID"
else
    print_result 1 "Error al crear promoción con contenido"
fi

wait_request

# ============================================
# TEST 7: POST /api/promociones
# Crear promoción con trigger de MONTO_MINIMO
# ============================================
print_section "TEST 7: Crear promoción con MONTO_MINIMO"

PROMO_MONTO_JSON='{
    "nombre": "Mega Descuento $10000",
    "descripcion": "20% de descuento en compras mayores a $10000",
    "prioridad": 1,
    "tipoEstrategia": "DESCUENTO_DIRECTO",
    "descuentoDirecto": {
        "modo": "PORCENTAJE",
        "valor": 20
    },
    "triggers": [
        {
            "tipo": "MONTO_MINIMO",
            "montoMinimo": 10000
        }
    ]
}'

echo "Request: POST $BASE_URL"
echo "Body:"
echo "$PROMO_MONTO_JSON" | jq '.'
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL" \
    -H "$CONTENT_TYPE" \
    -d "$PROMO_MONTO_JSON")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | head -n -1)

echo "Response Code: $HTTP_CODE"
echo "Response Body:"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

if [ "$HTTP_CODE" -eq 201 ]; then
    CREATED_ID=$(echo "$BODY" | jq -r '.id' 2>/dev/null)
    print_result 0 "Promoción con MONTO_MINIMO creada. ID: $CREATED_ID"
    PROMO_MONTO_ID="$CREATED_ID"
else
    print_result 1 "Error al crear promoción con monto mínimo"
fi

wait_request

# ============================================
# TEST 8: POST /api/promociones - Error: nombre duplicado
# Validar que no se permiten nombres duplicados
# ============================================
print_section "TEST 8: Validar error por nombre duplicado"

DUPLICADO_JSON='{
    "nombre": "2x1 en Cervezas",
    "descripcion": "Intento de duplicado",
    "prioridad": 5,
    "tipoEstrategia": "CANTIDAD_FIJA",
    "cantidadFija": {
        "cantidadLlevas": 2,
        "cantidadPagas": 1
    },
    "triggers": [
        {
            "tipo": "TEMPORAL",
            "fechaDesde": "2026-03-01",
            "fechaHasta": "2026-03-31"
        }
    ]
}'

echo "Request: POST $BASE_URL (debe fallar)"
echo "Body:"
echo "$DUPLICADO_JSON" | jq '.'
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL" \
    -H "$CONTENT_TYPE" \
    -d "$DUPLICADO_JSON")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | head -n -1)

echo "Response Code: $HTTP_CODE"
echo "Response Body:"
echo "$BODY"

if [ "$HTTP_CODE" -ge 400 ]; then
    print_result 0 "Validación correcta: nombre duplicado rechazado"
else
    print_result 1 "Error: debería rechazar nombre duplicado"
fi

wait_request

# ============================================
# TEST 9: PUT /api/promociones/{id}
# Actualizar promoción existente
# ============================================
print_section "TEST 9: Actualizar promoción 20% OFF en Pizzas"

ACTUALIZAR_JSON='{
    "nombre": "20% OFF en Pizzas EDITADO",
    "descripcion": "Descuento actualizado a 25% en todas las pizzas",
    "prioridad": 6,
    "triggers": [
        {
            "tipo": "TEMPORAL",
            "fechaDesde": "2026-02-01",
            "fechaHasta": "2026-03-31"
        },
        {
            "tipo": "MONTO_MINIMO",
            "montoMinimo": 2000
        }
    ]
}'

echo "Request: PUT $BASE_URL/$PROMO_20OFF_ID"
echo "Body:"
echo "$ACTUALIZAR_JSON" | jq '.'
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/$PROMO_20OFF_ID" \
    -H "$CONTENT_TYPE" \
    -d "$ACTUALIZAR_JSON")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | head -n -1)

echo "Response Code: $HTTP_CODE"
echo "Response Body:"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

if [ "$HTTP_CODE" -eq 200 ]; then
    NUEVO_NOMBRE=$(echo "$BODY" | jq -r '.nombre' 2>/dev/null)
    NUM_TRIGGERS=$(echo "$BODY" | jq '.triggers | length' 2>/dev/null)
    print_result 0 "Actualización exitosa. Nombre: $NUEVO_NOMBRE, Triggers: $NUM_TRIGGERS"
else
    print_result 1 "Error al actualizar promoción"
fi

wait_request

# ============================================
# TEST 10: PUT /api/promociones/{id}
# Actualizar solo nombre y descripción (sin triggers)
# ============================================
print_section "TEST 10: Actualizar solo nombre sin cambiar triggers"

ACTUALIZAR_PARCIAL_JSON='{
    "nombre": "Happy Hour Hamburguesas VIP",
    "descripcion": "Descripción actualizada",
    "prioridad": 20,
    "triggers": [
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
            "productosRequeridos": ["'$PRODUCTO_HAMBURGUESA'"]
        }
    ]
}'

echo "Request: PUT $BASE_URL/$PROMO_HAPPY_ID"
echo "Body:"
echo "$ACTUALIZAR_PARCIAL_JSON" | jq '.'
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/$PROMO_HAPPY_ID" \
    -H "$CONTENT_TYPE" \
    -d "$ACTUALIZAR_PARCIAL_JSON")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | head -n -1)

echo "Response Code: $HTTP_CODE"
echo "Response Body:"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

if [ "$HTTP_CODE" -eq 200 ]; then
    print_result 0 "Actualización parcial exitosa"
else
    print_result 1 "Error en actualización parcial"
fi

wait_request

# ============================================
# TEST 11: PUT /api/promociones/{id} - Error: ID inexistente
# Validar error al intentar actualizar promoción inexistente
# ============================================
print_section "TEST 11: Validar error por ID inexistente"

ID_FALSO="99999999-9999-9999-9999-999999999999"

ACTUALIZAR_FALSO_JSON='{
    "nombre": "No Existe",
    "descripcion": "Esto debe fallar",
    "prioridad": 1,
    "triggers": [
        {
            "tipo": "TEMPORAL",
            "fechaDesde": "2026-02-01",
            "fechaHasta": "2026-02-28"
        }
    ]
}'

echo "Request: PUT $BASE_URL/$ID_FALSO (debe fallar)"
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/$ID_FALSO" \
    -H "$CONTENT_TYPE" \
    -d "$ACTUALIZAR_FALSO_JSON")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | head -n -1)

echo "Response Code: $HTTP_CODE"
echo "Response Body:"
echo "$BODY"

if [ "$HTTP_CODE" -ge 400 ]; then
    print_result 0 "Validación correcta: ID inexistente rechazado"
else
    print_result 1 "Error: debería rechazar ID inexistente"
fi

wait_request

# ============================================
# TEST 12: DELETE /api/promociones/{id}
# Eliminar (desactivar) una promoción
# ============================================
print_section "TEST 12: Eliminar (desactivar) promoción Combo Ensaladas"

echo "Request: DELETE $BASE_URL/$PROMO_COMBO_ID"
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL/$PROMO_COMBO_ID")
HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)

echo "Response Code: $HTTP_CODE"

if [ "$HTTP_CODE" -eq 204 ]; then
    print_result 0 "Promoción eliminada (soft delete) exitosamente"
    
    # Verificar que ahora está inactiva
    echo ""
    echo "Verificando estado después de eliminación..."
    VERIFY_RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/$PROMO_COMBO_ID")
    VERIFY_HTTP_CODE=$(echo "$VERIFY_RESPONSE" | tail -n 1)
    VERIFY_BODY=$(echo "$VERIFY_RESPONSE" | head -n -1)
    
    if [ "$VERIFY_HTTP_CODE" -eq 200 ]; then
        ESTADO=$(echo "$VERIFY_BODY" | jq -r '.estado' 2>/dev/null)
        if [ "$ESTADO" = "INACTIVA" ]; then
            print_result 0 "Estado verificado: INACTIVA"
        else
            print_result 1 "Error: el estado debería ser INACTIVA"
        fi
    fi
else
    print_result 1 "Error al eliminar promoción"
fi

wait_request

# ============================================
# TEST 13: DELETE /api/promociones/{id} - Error: ID inexistente
# Validar error al eliminar promoción inexistente
# ============================================
print_section "TEST 13: Validar error al eliminar ID inexistente"

echo "Request: DELETE $BASE_URL/$ID_FALSO (debe fallar)"
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL/$ID_FALSO")
HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | head -n -1)

echo "Response Code: $HTTP_CODE"
echo "Response Body:"
echo "$BODY"

if [ "$HTTP_CODE" -ge 400 ]; then
    print_result 0 "Validación correcta: eliminación de ID inexistente rechazada"
else
    print_result 1 "Error: debería rechazar ID inexistente"
fi

wait_request

# ============================================
# TEST 14: Verificar promoción creada en TEST 5
# GET del miércoles de descuento
# ============================================
if [ ! -z "$NUEVA_PROMO_ID" ]; then
    print_section "TEST 14: Verificar promoción creada (Miércoles de Descuento)"
    
    echo "Request: GET $BASE_URL/$NUEVA_PROMO_ID"
    echo ""
    
    RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/$NUEVA_PROMO_ID")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    echo "Response Code: $HTTP_CODE"
    echo "Response Body:"
    echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
    
    if [ "$HTTP_CODE" -eq 200 ]; then
        NOMBRE=$(echo "$BODY" | jq -r '.nombre' 2>/dev/null)
        print_result 0 "Promoción recuperada: $NOMBRE"
    else
        print_result 1 "Error al recuperar promoción creada"
    fi
fi

# ============================================
# RESUMEN FINAL
# ============================================
print_section "RESUMEN DE TESTS"

echo -e "${YELLOW}Tests ejecutados:${NC}"
echo "  ✓ GET /api/promociones (listar todas)"
echo "  ✓ GET /api/promociones?estado=ACTIVA (filtrar activas)"
echo "  ✓ GET /api/promociones?estado=INACTIVA (filtrar inactivas)"
echo "  ✓ GET /api/promociones/{id} (detalle)"
echo "  ✓ POST /api/promociones (crear - TEMPORAL)"
echo "  ✓ POST /api/promociones (crear - CONTENIDO)"
echo "  ✓ POST /api/promociones (crear - MONTO_MINIMO)"
echo "  ✓ POST validación nombre duplicado (error esperado)"
echo "  ✓ PUT /api/promociones/{id} (actualizar completo)"
echo "  ✓ PUT /api/promociones/{id} (actualizar parcial)"
echo "  ✓ PUT validación ID inexistente (error esperado)"
echo "  ✓ DELETE /api/promociones/{id} (soft delete)"
echo "  ✓ DELETE validación ID inexistente (error esperado)"
echo "  ✓ Verificación de promoción creada"
echo ""
echo -e "${GREEN}Suite de tests completada${NC}"
echo ""
echo -e "${YELLOW}Nota:${NC} Para cargar datos de prueba ejecutar:"
echo "  ./ff staging seed-promociones.sql"
echo ""
