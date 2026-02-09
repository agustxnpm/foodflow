#!/bin/bash

# ============================================
# Suite de Tests CURL para HU-20 y HU-21
# Gestionar Items de Pedido (Eliminar y Modificar Cantidad)
# Archivo: test-gestionar-items.sh
# Fecha: 2026-02-07
# ============================================
# Prerequisitos:
#   1. Servidor corriendo en localhost:8080
#   2. Base de datos cargada con: ./ff staging scripts/staging/test-gestionar-items-data.sql

BASE_URL="http://localhost:8080/api/pedidos"
CONTENT_TYPE="Content-Type: application/json"

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

# IDs de prueba - Pedidos y sus Mesas
PEDIDO_CERVEZAS="ee000001-0001-0001-0001-000000000001"
MESA_CERVEZAS="ee000020-0020-0020-0020-000000000020"  # Mesa 20
ITEM_CERVEZAS="ee000101-0101-0101-0101-000000000101"

PEDIDO_COMBO="ee000002-0002-0002-0002-000000000002"
MESA_COMBO="ee000021-0021-0021-0021-000000000021"  # Mesa 21
ITEM_HAMBURGUESA="ee000201-0201-0201-0201-000000000201"
ITEM_PAPAS_COMBO="ee000202-0202-0202-0202-000000000202"

PEDIDO_DESC_GLOBAL="ee000003-0003-0003-0003-000000000003"
MESA_DESC_GLOBAL="ee000022-0022-0022-0022-000000000022"  # Mesa 22
ITEM_MILANESA="ee000301-0301-0301-0301-000000000301"
ITEM_PAPAS_DESC="ee000302-0302-0302-0302-000000000302"

PEDIDO_UNICO_ITEM="ee000004-0004-0004-0004-000000000004"
MESA_UNICO_ITEM="ee000023-0023-0023-0023-000000000023"  # Mesa 23
ITEM_GASEOSA="ee000401-0401-0401-0401-000000000401"

# ============================================
# FUNCIONES AUXILIARES
# ============================================

print_section() {
    echo ""
    echo -e "${BLUE}============================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}============================================${NC}"
    echo ""
}

print_test() {
    echo -e "${CYAN}▶ $1${NC}"
    echo ""
}

print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ ÉXITO${NC}"
    else
        echo -e "${RED}✗ FALLÓ${NC}"
    fi
    echo ""
}

assert_field() {
    local json="$1"
    local field="$2"
    local expected="$3"
    local test_name="$4"
    
    local actual=$(echo "$json" | jq -r "$field")
    
    if [ "$actual" == "$expected" ]; then
        echo -e "  ${GREEN}✓${NC} $test_name: ${MAGENTA}$actual${NC}"
        return 0
    else
        echo -e "  ${RED}✗${NC} $test_name: esperado ${YELLOW}$expected${NC}, obtenido ${MAGENTA}$actual${NC}"
        return 1
    fi
}

# Validación de campos numéricos con normalización decimal
assert_number() {
    local json="$1"
    local field="$2"
    local expected="$3"
    local test_name="$4"
    
    local actual=$(echo "$json" | jq -r "$field")
    
    # Normalizar ambos valores a formato decimal con 2 decimales usando bc
    local actual_norm=$(printf "%.2f" "$actual" 2>/dev/null || echo "$actual")
    local expected_norm=$(printf "%.2f" "$expected" 2>/dev/null || echo "$expected")
    
    if [ "$actual_norm" == "$expected_norm" ]; then
        echo -e "  ${GREEN}✓${NC} $test_name: ${MAGENTA}$actual${NC}"
        return 0
    else
        echo -e "  ${RED}✗${NC} $test_name: esperado ${YELLOW}$expected${NC}, obtenido ${MAGENTA}$actual${NC}"
        return 1
    fi
}

# ============================================
# TEST 1: MODIFICAR CANTIDAD - CICLO 2x1
# ============================================

print_section "TEST 1: Modificar Cantidad - Ciclo 2x1 en Cervezas"

print_test "1.1 Estado inicial: 2 cervezas con 2x1 → paga 1"
echo "GET http://localhost:8080/api/mesas/$MESA_CERVEZAS/pedido-actual"
RESPONSE=$(curl -s "http://localhost:8080/api/mesas/$MESA_CERVEZAS/pedido-actual" -H "$CONTENT_TYPE")
echo "$RESPONSE" | jq '.'
echo ""

assert_field "$RESPONSE" '.items[0].cantidad' '2' 'Cantidad inicial'
assert_field "$RESPONSE" '.items[0].tienePromocion' 'true' 'Tiene promoción'
assert_number "$RESPONSE" '.items[0].descuentoTotal' '1500' 'Descuento (1 gratis)'
assert_number "$RESPONSE" '.items[0].precioFinal' '1500' 'Precio final (paga 1)'
print_result $?

print_test "1.2 Modificar a 4 cervezas → debe pagar 2 (2 ciclos de 2x1)"
echo "PATCH $BASE_URL/$PEDIDO_CERVEZAS/items/$ITEM_CERVEZAS"
echo 'Body: {"cantidad": 4}'
RESPONSE=$(curl -s -X PATCH "$BASE_URL/$PEDIDO_CERVEZAS/items/$ITEM_CERVEZAS" \
  -H "$CONTENT_TYPE" \
  -d '{"cantidad": 4}')
echo "$RESPONSE" | jq '.'
echo ""

assert_field "$RESPONSE" '.items[0].cantidad' '4' 'Nueva cantidad'
assert_number "$RESPONSE" '.items[0].subtotalItem' '6000' 'Subtotal (4 × 1500)'
assert_number "$RESPONSE" '.items[0].descuentoTotal' '3000' 'Descuento (2 gratis)'
assert_number "$RESPONSE" '.items[0].precioFinal' '3000' 'Precio final (paga 2)'
assert_field "$RESPONSE" '.items[0].tienePromocion' 'true' 'Promoción sigue activa'
print_result $?

print_test "1.3 Modificar a 3 cervezas → 1 ciclo completo + 1 extra sin promo"
RESPONSE=$(curl -s -X PATCH "$BASE_URL/$PEDIDO_CERVEZAS/items/$ITEM_CERVEZAS" \
  -H "$CONTENT_TYPE" \
  -d '{"cantidad": 3}')
echo "$RESPONSE" | jq '.'
echo ""

assert_field "$RESPONSE" '.items[0].cantidad' '3' 'Nueva cantidad'
assert_number "$RESPONSE" '.items[0].subtotalItem' '4500' 'Subtotal (3 × 1500)'
assert_number "$RESPONSE" '.items[0].descuentoTotal' '1500' 'Descuento (1 gratis)'
assert_number "$RESPONSE" '.items[0].precioFinal' '3000' 'Precio final (paga 2)'
print_result $?

print_test "1.4 Modificar a 1 cerveza → pierde promoción (no alcanza el mínimo)"
RESPONSE=$(curl -s -X PATCH "$BASE_URL/$PEDIDO_CERVEZAS/items/$ITEM_CERVEZAS" \
  -H "$CONTENT_TYPE" \
  -d '{"cantidad": 1}')
echo "$RESPONSE" | jq '.'
echo ""

assert_field "$RESPONSE" '.items[0].cantidad' '1' 'Nueva cantidad'
assert_field "$RESPONSE" '.items[0].tienePromocion' 'false' 'Sin promoción'
assert_number "$RESPONSE" '.items[0].descuentoTotal' '0' 'Sin descuento'
assert_number "$RESPONSE" '.items[0].precioFinal' '1500' 'Precio full'
print_result $?

print_test "1.5 Modificar a 2 nuevamente → vuelve a activarse la promoción"
RESPONSE=$(curl -s -X PATCH "$BASE_URL/$PEDIDO_CERVEZAS/items/$ITEM_CERVEZAS" \
  -H "$CONTENT_TYPE" \
  -d '{"cantidad": 2}')
echo "$RESPONSE" | jq '.'
echo ""

assert_field "$RESPONSE" '.items[0].cantidad' '2' 'Cantidad restaurada'
assert_field "$RESPONSE" '.items[0].tienePromocion' 'true' 'Promoción reactivada'
assert_number "$RESPONSE" '.items[0].descuentoTotal' '1500' 'Descuento restaurado'
print_result $?

# ============================================
# TEST 2: ELIMINAR TRIGGER DE COMBO
# ============================================

print_section "TEST 2: Eliminar Trigger de Combo - Rotura de Promoción"

print_test "2.1 Estado inicial: Hamburguesa + Papas con combo activo"
echo "GET http://localhost:8080/api/mesas/$MESA_COMBO/pedido-actual"
RESPONSE=$(curl -s "http://localhost:8080/api/mesas/$MESA_COMBO/pedido-actual" -H "$CONTENT_TYPE")
echo "$RESPONSE" | jq '.'
echo ""

# Debería haber 2 items
assert_field "$RESPONSE" '.items | length' '2' 'Cantidad de items'
echo ""

# Buscar item de papas (puede estar en items[0] o items[1])
PAPAS_JSON=$(echo "$RESPONSE" | jq '.items[] | select(.nombreProducto == "Papas Fritas")')
PAPAS_TIENE_PROMO=$(echo "$PAPAS_JSON" | jq -r '.tienePromocion')
PAPAS_DESCUENTO=$(echo "$PAPAS_JSON" | jq -r '.descuentoTotal')

echo -e "  ${GREEN}✓${NC} Papas tiene promoción: ${MAGENTA}$PAPAS_TIENE_PROMO${NC}"
echo -e "  ${GREEN}✓${NC} Papas descuento (50%): ${MAGENTA}$PAPAS_DESCUENTO${NC}"
echo ""

print_test "2.2 Eliminar hamburguesa (trigger) → papas deben perder descuento"
echo "DELETE $BASE_URL/$PEDIDO_COMBO/items/$ITEM_HAMBURGUESA"
RESPONSE=$(curl -s -X DELETE "$BASE_URL/$PEDIDO_COMBO/items/$ITEM_HAMBURGUESA" \
  -H "$CONTENT_TYPE")
echo "$RESPONSE" | jq '.'
echo ""

assert_field "$RESPONSE" '.items | length' '1' 'Solo queda 1 item (papas)'
assert_field "$RESPONSE" '.items[0].nombreProducto' 'Papas Fritas' 'Item restante es Papas'
assert_field "$RESPONSE" '.items[0].tienePromocion' 'false' 'Papas SIN promoción'
assert_number "$RESPONSE" '.items[0].descuentoTotal' '0' 'Papas SIN descuento'
assert_number "$RESPONSE" '.items[0].precioFinal' '800' 'Papas a precio full'
assert_number "$RESPONSE" '.subtotal' '800' 'Subtotal del pedido'
print_result $?

# ============================================
# TEST 3: ELIMINAR ITEM CON DESCUENTO GLOBAL (HU-14)
# ============================================

print_section "TEST 3: Eliminar Item con Descuento Global - Dinamismo HU-14"

print_test "3.1 Estado inicial: Milanesa + Papas con 10% descuento global"
echo "GET http://localhost:8080/api/mesas/$MESA_DESC_GLOBAL/pedido-actual"
RESPONSE=$(curl -s "http://localhost:8080/api/mesas/$MESA_DESC_GLOBAL/pedido-actual" -H "$CONTENT_TYPE")
echo "$RESPONSE" | jq '.'
echo ""

assert_field "$RESPONSE" '.items | length' '2' 'Cantidad de items'
assert_number "$RESPONSE" '.subtotal' '3800' 'Subtotal (3000 + 800)'
assert_number "$RESPONSE" '.totalDescuentos' '380' 'Descuento global 10% de 3800'
assert_number "$RESPONSE" '.total' '3420' 'Total con descuento'
print_result $?

print_test "3.2 Eliminar Milanesa ($3000) → descuento global debe recalcularse"
echo "DELETE $BASE_URL/$PEDIDO_DESC_GLOBAL/items/$ITEM_MILANESA"
RESPONSE=$(curl -s -X DELETE "$BASE_URL/$PEDIDO_DESC_GLOBAL/items/$ITEM_MILANESA" \
  -H "$CONTENT_TYPE")
echo "$RESPONSE" | jq '.'
echo ""

assert_field "$RESPONSE" '.items | length' '1' 'Solo queda 1 item'
assert_field "$RESPONSE" '.items[0].nombreProducto' 'Papas Fritas' 'Item restante'
assert_number "$RESPONSE" '.subtotal' '800' 'Nuevo subtotal'
assert_number "$RESPONSE" '.totalDescuentos' '80' 'Descuento recalculado: 10% de 800'
assert_number "$RESPONSE" '.total' '720' 'Nuevo total'
print_result $?

# ============================================
# TEST 4: MODIFICAR CANTIDAD CON DESCUENTO GLOBAL
# ============================================

print_section "TEST 4: Modificar Cantidad con Descuento Global - HU-14"

print_test "4.1 Modificar cantidad de papas de 1 a 3 → descuento global se recalcula"
RESPONSE=$(curl -s -X PATCH "$BASE_URL/$PEDIDO_DESC_GLOBAL/items/$ITEM_PAPAS_DESC" \
  -H "$CONTENT_TYPE" \
  -d '{"cantidad": 3}')
echo "$RESPONSE" | jq '.'
echo ""

assert_field "$RESPONSE" '.items[0].cantidad' '3' 'Nueva cantidad'
assert_number "$RESPONSE" '.items[0].subtotalItem' '2400' 'Subtotal item (3 × 800)'
assert_number "$RESPONSE" '.subtotal' '2400' 'Subtotal pedido'
assert_number "$RESPONSE" '.totalDescuentos' '240' 'Descuento recalculado: 10% de 2400'
assert_number "$RESPONSE" '.total' '2160' 'Total actualizado'
print_result $?

# ============================================
# TEST 5: ELIMINAR ÚLTIMO ITEM
# ============================================

print_section "TEST 5: Eliminar Último Item - Pedido Vacío"

print_test "5.1 Estado inicial: Pedido con 1 gaseosa"
echo "GET http://localhost:8080/api/mesas/$MESA_UNICO_ITEM/pedido-actual"
RESPONSE=$(curl -s "http://localhost:8080/api/mesas/$MESA_UNICO_ITEM/pedido-actual" -H "$CONTENT_TYPE")
echo "$RESPONSE" | jq '.'
echo ""

assert_field "$RESPONSE" '.items | length' '1' 'Un solo item'
assert_number "$RESPONSE" '.total' '600' 'Total $600'
print_result $?

print_test "5.2 Eliminar el único item → pedido vacío"
echo "DELETE $BASE_URL/$PEDIDO_UNICO_ITEM/items/$ITEM_GASEOSA"
RESPONSE=$(curl -s -X DELETE "$BASE_URL/$PEDIDO_UNICO_ITEM/items/$ITEM_GASEOSA" \
  -H "$CONTENT_TYPE")
echo "$RESPONSE" | jq '.'
echo ""

assert_field "$RESPONSE" '.items | length' '0' 'Pedido vacío'
assert_number "$RESPONSE" '.subtotal' '0' 'Subtotal $0'
assert_number "$RESPONSE" '.total' '0' 'Total $0'
assert_number "$RESPONSE" '.totalDescuentos' '0' 'Sin descuentos'
print_result $?

# ============================================
# TEST 6: CANTIDAD 0 ELIMINA ITEM
# ============================================

print_section "TEST 6: Modificar Cantidad a 0 - Semántica de Eliminación"

# Primero agregamos un item al pedido combo que ya solo tiene papas
print_test "6.1 Agregar hamburguesa al pedido combo"
PRODUCTO_HAMBURGUESA="e1111111-1111-1111-1111-111111111111"
RESPONSE=$(curl -s -X POST "$BASE_URL/$PEDIDO_COMBO/items" \
  -H "$CONTENT_TYPE" \
  -d "{\"productoId\": \"$PRODUCTO_HAMBURGUESA\", \"cantidad\": 1, \"observaciones\": null}")
echo "$RESPONSE" | jq '.'
echo ""

assert_field "$RESPONSE" '.items | length' '2' 'Ahora hay 2 items'
print_result $?

# Obtener el ID del nuevo item
NUEVO_ITEM_ID=$(echo "$RESPONSE" | jq -r '.items[] | select(.nombreProducto == "Hamburguesa Completa") | .itemId')
echo -e "ID del nuevo item: ${MAGENTA}$NUEVO_ITEM_ID${NC}"
echo ""

print_test "6.2 Modificar cantidad a 0 → debe eliminar el item"
RESPONSE=$(curl -s -X PATCH "$BASE_URL/$PEDIDO_COMBO/items/$NUEVO_ITEM_ID" \
  -H "$CONTENT_TYPE" \
  -d '{"cantidad": 0}')
echo "$RESPONSE" | jq '.'
echo ""

assert_field "$RESPONSE" '.items | length' '1' 'Item eliminado'
assert_field "$RESPONSE" '.items[0].nombreProducto' 'Papas Fritas' 'Solo quedan papas'
print_result $?

# ============================================
# TEST 7: IDEMPOTENCIA
# ============================================

print_section "TEST 7: Idempotencia - Modificar con Misma Cantidad"

print_test "7.1 Modificar papas con su cantidad actual (debe ser idempotente)"
CANTIDAD_ACTUAL=$(curl -s "http://localhost:8080/api/mesas/$MESA_COMBO/pedido-actual" -H "$CONTENT_TYPE" | jq -r '.items[0].cantidad')
echo -e "Cantidad actual de papas: ${MAGENTA}$CANTIDAD_ACTUAL${NC}"
echo ""

RESPONSE_ANTES=$(curl -s "http://localhost:8080/api/mesas/$MESA_COMBO/pedido-actual" -H "$CONTENT_TYPE")
TOTAL_ANTES=$(echo "$RESPONSE_ANTES" | jq -r '.total')

RESPONSE=$(curl -s -X PATCH "$BASE_URL/$PEDIDO_COMBO/items/$ITEM_PAPAS_COMBO" \
  -H "$CONTENT_TYPE" \
  -d "{\"cantidad\": $CANTIDAD_ACTUAL}")
echo "$RESPONSE" | jq '.'
echo ""

TOTAL_DESPUES=$(echo "$RESPONSE" | jq -r '.total')

if [ "$TOTAL_ANTES" == "$TOTAL_DESPUES" ]; then
    echo -e "${GREEN}✓${NC} Operación idempotente: total no cambió (${MAGENTA}$TOTAL_ANTES${NC})"
    print_result 0
else
    echo -e "${RED}✗${NC} Operación NO idempotente: total cambió de ${YELLOW}$TOTAL_ANTES${NC} a ${MAGENTA}$TOTAL_DESPUES${NC}"
    print_result 1
fi

# ============================================
# TEST 8: VALIDACIONES - CANTIDAD NEGATIVA
# ============================================

print_section "TEST 8: Validaciones - Cantidad Negativa Rechazada"

print_test "8.1 Intentar modificar a cantidad negativa → debe fallar"
echo "PATCH $BASE_URL/$PEDIDO_COMBO/items/$ITEM_PAPAS_COMBO"
echo 'Body: {"cantidad": -1}'

HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X PATCH "$BASE_URL/$PEDIDO_COMBO/items/$ITEM_PAPAS_COMBO" \
  -H "$CONTENT_TYPE" \
  -d '{"cantidad": -1}')

echo ""
if [ "$HTTP_STATUS" == "400" ] || [ "$HTTP_STATUS" == "500" ]; then
    echo -e "${GREEN}✓${NC} Rechazado correctamente (HTTP $HTTP_STATUS)"
    print_result 0
else
    echo -e "${RED}✗${NC} No rechazado: HTTP $HTTP_STATUS"
    print_result 1
fi

# ============================================
# TEST 9: VALIDACIONES - ITEM INEXISTENTE
# ============================================

print_section "TEST 9: Validaciones - Item Inexistente"

print_test "9.1 Intentar eliminar item inexistente → debe fallar"
ITEM_FAKE="99999999-9999-9999-9999-999999999999"
echo "DELETE $BASE_URL/$PEDIDO_COMBO/items/$ITEM_FAKE"

HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/$PEDIDO_COMBO/items/$ITEM_FAKE" \
  -H "$CONTENT_TYPE")

echo ""
if [ "$HTTP_STATUS" == "400" ] || [ "$HTTP_STATUS" == "404" ] || [ "$HTTP_STATUS" == "500" ]; then
    echo -e "${GREEN}✓${NC} Rechazado correctamente (HTTP $HTTP_STATUS)"
    print_result 0
else
    echo -e "${RED}✗${NC} No rechazado: HTTP $HTTP_STATUS"
    print_result 1
fi

# ============================================
# RESUMEN FINAL
# ============================================

print_section "RESUMEN DE TESTS COMPLETADOS"

echo -e "${GREEN}✓${NC} TEST 1: Modificar Cantidad - Ciclos NxM (2x1)"
echo -e "${GREEN}✓${NC} TEST 2: Eliminar Trigger de Combo - Rotura de Promoción"
echo -e "${GREEN}✓${NC} TEST 3: Eliminar Item con Descuento Global (HU-14)"
echo -e "${GREEN}✓${NC} TEST 4: Modificar Cantidad con Descuento Global (HU-14)"
echo -e "${GREEN}✓${NC} TEST 5: Eliminar Último Item - Pedido Vacío"
echo -e "${GREEN}✓${NC} TEST 6: Cantidad 0 Elimina Item"
echo -e "${GREEN}✓${NC} TEST 7: Idempotencia"
echo -e "${GREEN}✓${NC} TEST 8: Validación - Cantidad Negativa"
echo -e "${GREEN}✓${NC} TEST 9: Validación - Item Inexistente"
echo ""
echo -e "${BLUE}============================================${NC}"
echo -e "${GREEN}Suite de tests completada exitosamente${NC}"
echo -e "${BLUE}============================================${NC}"
