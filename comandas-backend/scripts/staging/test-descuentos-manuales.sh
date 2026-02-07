#!/bin/bash

# ============================================
# Suite de Tests CURL para HU-14: Descuentos Manuales
# Archivo: test-descuentos-manuales.sh
# Fecha: 2026-02-06
# ============================================
# Prerequisitos:
#   1. Servidor corriendo en localhost:8080
#   2. Base de datos cargada con seed de prueba

echo "Limpiando descuentos previos de la base de datos..."
echo "→ Ejecutando limpieza SQL..."

# Crear archivo temporal con SQL de limpieza
CLEANUP_SQL=$(mktemp)
cat > "$CLEANUP_SQL" <<'EOF'
-- Limpiar descuentos globales en la tabla pedidos (usando UPDATE porque son campos embebidos)
UPDATE pedidos 
SET 
  desc_global_porcentaje = NULL,
  desc_global_razon = NULL,
  desc_global_usuario_id = NULL,
  desc_global_fecha = NULL
WHERE id IN (
  'dd000001-0001-0001-0001-000000000001',
  'dd000002-0002-0002-0002-000000000002',
  'dd000003-0003-0003-0003-000000000003'
);

-- Limpiar descuentos manuales por ítem
UPDATE items_pedido 
SET 
  desc_manual_porcentaje = NULL,
  desc_manual_razon = NULL,
  desc_manual_usuario_id = NULL,
  desc_manual_fecha = NULL,
  monto_descuento = 0.00 -- Reseteamos el monto acumulado
WHERE pedido_id IN (
  'dd000001-0001-0001-0001-000000000001',
  'dd000002-0002-0002-0002-000000000002',
  'dd000003-0003-0003-0003-000000000003'
);

-- Borrar items adicionales que pudieron quedar de ejecuciones previas (en el pedido vacio)
DELETE FROM items_pedido
WHERE pedido_id = 'dd000002-0002-0002-0002-000000000002';
EOF

# Ejecutar limpieza
../../ff staging "$CLEANUP_SQL"

# Borrar archivo temporal
rm -f "$CLEANUP_SQL"

echo "✓ Limpieza completada"
echo ""

BASE_URL="http://localhost:8080/api/pedidos"
CONTENT_TYPE="Content-Type: application/json"

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# IDs de prueba
PEDIDO_CON_ITEMS="dd000001-0001-0001-0001-000000000001"
PEDIDO_VACIO="dd000002-0002-0002-0002-000000000002"
PEDIDO_CERVEZA="dd000003-0003-0003-0003-000000000003"

ITEM_PIZZA="dd000011-0011-0011-0011-000000000011"
ITEM_EMPANADAS="dd000012-0012-0012-0012-000000000012"
ITEM_CERVEZA="dd000013-0013-0013-0013-000000000013"

PRODUCTO_PIZZA="d1111111-1111-1111-1111-111111111111"
USUARIO_MOZO="550e8400-e29b-41d4-a716-446655440000"

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
        echo -e "${GREEN}✓ $2${NC}"
    else
        echo -e "${RED}✗ $2${NC}"
    fi
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

wait_request() {
    sleep 0.5
}

validate_number() {
    local actual=$1
    local expected=$2
    local tolerance=${3:-0.01}
    
    local diff=$(echo "$actual - $expected" | bc -l)
    local abs_diff=$(echo "$diff" | sed 's/-//')
    
    if (( $(echo "$abs_diff < $tolerance" | bc -l) )); then
        return 0
    else
        return 1
    fi
}

# ============================================
# TEST 1: DESCUENTO GLOBAL 10% SOBRE PEDIDO
# ============================================
print_section "TEST 1: Aplicar Descuento Global del 10%"

print_test "Escenario: Pedido con 2 ítems (Pizza $2500 + Empanadas $3600). Total Bruto: $6100."
print_test "Aplicar descuento global del 10% → $610 de descuento → Total: $5490"

REQUEST_BODY=$(cat <<EOF
{
  "porcentaje": 10,
  "razon": "Cliente frecuente - Test HU-14",
  "usuarioId": "$USUARIO_MOZO"
}
EOF
)

echo "Request: POST $BASE_URL/$PEDIDO_CON_ITEMS/descuento-manual"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST -H "$CONTENT_TYPE" -d "$REQUEST_BODY" "$BASE_URL/$PEDIDO_CON_ITEMS/descuento-manual")
HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | head -n -1)

echo "Response Code: $HTTP_CODE"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

if [ "$HTTP_CODE" -eq 200 ]; then
    TOTAL_FINAL=$(echo "$BODY" | jq -r '.totalFinal' 2>/dev/null)
    DESCUENTO_GLOBAL=$(echo "$BODY" | jq -r '.montoDescuentoGlobal' 2>/dev/null)
    SUBTOTAL=$(echo "$BODY" | jq -r '.subtotalBruto' 2>/dev/null)
    
    # VALORES ESPERADOS (Ejecución Limpia):
    # Subtotal Bruto: 6100.00
    # Descuento Global 10%: 610.00
    # Total Final: 5490.00
    
    validate_number "$SUBTOTAL" "6100.00" && SUBTOTAL_OK=1 || SUBTOTAL_OK=0
    validate_number "$DESCUENTO_GLOBAL" "610.00" && DESC_OK=1 || DESC_OK=0
    validate_number "$TOTAL_FINAL" "5490.00" && TOTAL_OK=1 || TOTAL_OK=0
    
    [ $SUBTOTAL_OK -eq 1 ] && print_result 0 "Subtotal correcto: $SUBTOTAL" || print_result 1 "Subtotal incorrecto: $SUBTOTAL (esperado 6100.00)"
    [ $DESC_OK -eq 1 ] && print_result 0 "Descuento 10% correcto: $DESCUENTO_GLOBAL" || print_result 1 "Descuento incorrecto: $DESCUENTO_GLOBAL (esperado 610.00)"
    [ $TOTAL_OK -eq 1 ] && print_result 0 "Total final correcto: $TOTAL_FINAL" || print_result 1 "Total final incorrecto: $TOTAL_FINAL (esperado 5490.00)"
else
    print_result 1 "Error HTTP $HTTP_CODE"
fi
wait_request

# ============================================
# TEST 2: DESCUENTO POR ÍTEM 20% SOBRE PIZZA
# ============================================
print_section "TEST 2: Aplicar Descuento del 20% a un Ítem Específico"
print_test "Escenario: Pizza $2500 → Descuento 20% (-$500) → $2000"
print_test "DINAMISMO: El descuento global se recalcula sobre la nueva base gravable."
print_test "Base Gravable: $2000 (Pizza) + $3600 (Empanadas) = $5600"
print_test "Descuento Global 10%: $560 (sobre $5600)"
print_test "Total Final: $5600 - $560 = $5040"

REQUEST_BODY=$(cat <<EOF
{
  "porcentaje": 20,
  "razon": "Compensación cocina",
  "usuarioId": "$USUARIO_MOZO"
}
EOF
)

echo "Request: POST $BASE_URL/$PEDIDO_CON_ITEMS/items/$ITEM_PIZZA/descuento-manual"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST -H "$CONTENT_TYPE" -d "$REQUEST_BODY" "$BASE_URL/$PEDIDO_CON_ITEMS/items/$ITEM_PIZZA/descuento-manual")
HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | head -n -1)

echo "Response Code: $HTTP_CODE"
echo "$BODY" | jq '.' 2>/dev/null

if [ "$HTTP_CODE" -eq 200 ]; then
    # Validar el ítem
    ITEM_DATA=$(echo "$BODY" | jq --arg itemId "$ITEM_PIZZA" '.items[] | select(.itemId == $itemId)')
    PRECIO_FINAL_ITEM=$(echo "$ITEM_DATA" | jq -r '.precioFinal')
    DESC_MANUAL_ITEM=$(echo "$ITEM_DATA" | jq -r '.montoDescuentoManual')
    
    # Validar efecto colateral en Global
    DESC_GLOBAL_RECALCULADO=$(echo "$BODY" | jq -r '.montoDescuentoGlobal')
    TOTAL_FINAL_RECALCULADO=$(echo "$BODY" | jq -r '.totalFinal')

    # LÓGICA DE NEGOCIO ESPERADA (HU-14 - Jerarquía de Descuentos):
    # 1. Descuento Manual ítem Pizza: 20% de $2500 = $500 → Precio final Pizza: $2000
    # 2. Base Gravable = $2000 (Pizza) + $3600 (Empanadas) = $5600
    # 3. Descuento Global 10% (dinámico): 10% de $5600 = $560
    # 4. Total Final: $5600 - $560 = $5040

    validate_number "$DESC_MANUAL_ITEM" "500.00" && ITEM_DESC_OK=1 || ITEM_DESC_OK=0
    validate_number "$DESC_GLOBAL_RECALCULADO" "560.00" && GLOBAL_DESC_OK=1 || GLOBAL_DESC_OK=0
    validate_number "$TOTAL_FINAL_RECALCULADO" "5040.00" && TOTAL_OK=1 || TOTAL_OK=0
    
    [ $ITEM_DESC_OK -eq 1 ] && print_result 0 "Descuento Item Pizza (20%): $DESC_MANUAL_ITEM" || print_result 1 "Error Desc Item: $DESC_MANUAL_ITEM (esp 500.00)"
    [ $GLOBAL_DESC_OK -eq 1 ] && print_result 0 "✨ Dinamismo: Global Recalculado (10% de 5600): $DESC_GLOBAL_RECALCULADO" || print_result 1 "Error Desc Global: $DESC_GLOBAL_RECALCULADO (esp 560.00)"
    [ $TOTAL_OK -eq 1 ] && print_result 0 "Total Final Recalculado: $TOTAL_FINAL_RECALCULADO" || print_result 1 "Error Total: $TOTAL_FINAL_RECALCULADO (esp 5040.00)"
else
    print_result 1 "Error HTTP $HTTP_CODE"
fi
wait_request

# ============================================
# TEST 3: SOBRESCRITURA DE DESCUENTO GLOBAL
# ============================================
print_section "TEST 3: Sobrescribir Descuento Global (10% → 15%)"
print_test "Escenario: Pedido Cerveza ($2000). Aplicar 10%, luego sobrescribir con 15%."

# Primero aplicar 10%
curl -s -X POST -H "$CONTENT_TYPE" -d '{"porcentaje":10,"razon":"Init","usuarioId":"'$USUARIO_MOZO'"}' "$BASE_URL/$PEDIDO_CERVEZA/descuento-manual" > /dev/null
print_warning "Aplicado descuento inicial del 10% ($200)"

# Ahora sobrescribir con 15%
REQUEST_BODY=$(cat <<EOF
{
  "porcentaje": 15,
  "razon": "Ajuste VIP",
  "usuarioId": "$USUARIO_MOZO"
}
EOF
)

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST -H "$CONTENT_TYPE" -d "$REQUEST_BODY" "$BASE_URL/$PEDIDO_CERVEZA/descuento-manual")
HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | head -n -1)

echo "Response Code: $HTTP_CODE"
echo "$BODY" | jq '.' 2>/dev/null

if [ "$HTTP_CODE" -eq 200 ]; then
    DESC_GLOBAL=$(echo "$BODY" | jq -r '.montoDescuentoGlobal')
    TOTAL=$(echo "$BODY" | jq -r '.totalFinal')
    
    # Cerveza: $2000 × 15% = $300 descuento → Total: $1700
    validate_number "$DESC_GLOBAL" "300.00" && D_OK=1 || D_OK=0
    validate_number "$TOTAL" "1700.00" && T_OK=1 || T_OK=0
    
    [ $D_OK -eq 1 ] && print_result 0 "Descuento Global 15% (sobrescrito): $DESC_GLOBAL" || print_result 1 "Error: $DESC_GLOBAL (esperado 300.00)"
    [ $T_OK -eq 1 ] && print_result 0 "Total Final: $TOTAL" || print_result 1 "Error: $TOTAL (esperado 1700.00)"
else
    print_result 1 "Error HTTP $HTTP_CODE"
fi
wait_request

# ============================================
# TEST 4: DINAMISMO - AGREGAR ÍTEM A PEDIDO CON DESCUENTO GLOBAL
# ============================================
print_section "TEST 4: Dinamismo - Agregar Ítem a Pedido con Descuento Global"
print_test "Escenario CRÍTICO: Pedido vacío con 10% global → Agregar Pizza $2500"
print_test "El DTO debe devolver el total YA DESCONTADO (dinamismo instantáneo)"
print_test "Esperado: Subtotal $2500 → Descuento 10% ($250) → Total: $2250"

# 1. Aplicar 10% global a pedido vacío
curl -s -X POST -H "$CONTENT_TYPE" -d '{"porcentaje":10,"razon":"Pre-item","usuarioId":"'$USUARIO_MOZO'"}' "$BASE_URL/$PEDIDO_VACIO/descuento-manual" > /dev/null
print_warning "Descuento global 10% aplicado a pedido vacío (descuento actual: $0)"

# 2. Agregar Pizza ($2500) mediante endpoint /items
REQUEST_ITEM=$(cat <<EOF
{
  "productoId": "$PRODUCTO_PIZZA",
  "cantidad": 1
}
EOF
)

echo "Request: POST $BASE_URL/$PEDIDO_VACIO/items"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST -H "$CONTENT_TYPE" -d "$REQUEST_ITEM" "$BASE_URL/$PEDIDO_VACIO/items")
HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | head -n -1)

echo "Response Code: $HTTP_CODE"
echo "$BODY" | jq '.' 2>/dev/null

if [ "$HTTP_CODE" -eq 200 ]; then
    SUBTOTAL=$(echo "$BODY" | jq -r '.subtotal')
    TOTAL_DESCUENTOS=$(echo "$BODY" | jq -r '.totalDescuentos')
    TOTAL=$(echo "$BODY" | jq -r '.total')
    
    # VALIDACIÓN DEL FIX (HU-14):
    # El endpoint /items usa AgregarProductoResponse.fromDomain()
    # que AHORA llama a pedido.calcularTotal() en vez de calcular manualmente.
    # Esperado:
    # - Subtotal: 2500.00 (bruto)
    # - Total Descuentos: 250.00 (10% global dinámico)
    # - Total: 2250.00 (neto con descuento aplicado)
    
    validate_number "$SUBTOTAL" "2500.00" && S_OK=1 || S_OK=0
    validate_number "$TOTAL_DESCUENTOS" "250.00" && D_OK=1 || D_OK=0
    validate_number "$TOTAL" "2250.00" && T_OK=1 || T_OK=0
    
    [ $S_OK -eq 1 ] && print_result 0 "Subtotal: $SUBTOTAL" || print_result 1 "Error Subtotal: $SUBTOTAL (esperado 2500.00)"
    [ $D_OK -eq 1 ] && print_result 0 "Total Descuentos (10% dinámico): $TOTAL_DESCUENTOS" || print_result 1 "Error Descuentos: $TOTAL_DESCUENTOS (esperado 250.00)"
    
    if [ $T_OK -eq 1 ]; then
        print_result 0 "🎯 FIX EXITOSO - Dinamismo OK: Total refleja descuento global ($2250.00)"
    else
        print_result 1 "❌ BUG NO RESUELTO: Total $TOTAL (Esperado 2250.00) - El DTO sigue ignorando descuento global"
    fi
else
    print_result 1 "Error HTTP $HTTP_CODE"
fi
wait_request

# ============================================
# TEST 5 y 6 (Validaciones de Entrada)
# ============================================
print_section "TEST 5 & 6: Validaciones de Entrada"

# Test 5: Rechazar porcentaje > 100
print_test "Test 5: Validar rechazo de porcentaje 150%"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST -H "$CONTENT_TYPE" -d '{"porcentaje":150,"razon":"Fail","usuarioId":"'$USUARIO_MOZO'"}' "$BASE_URL/$PEDIDO_CERVEZA/descuento-manual")
HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
if [ "$HTTP_CODE" -eq 400 ]; then 
    print_result 0 "Rechazó correctamente porcentaje 150%"
else 
    print_result 1 "Falló validación: aceptó 150% (HTTP $HTTP_CODE)"
fi

wait_request

# Test 6: Aceptar razón vacía (permitido según reglas de negocio)
print_test "Test 6: Validar aceptación de razón vacía"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST -H "$CONTENT_TYPE" -d '{"porcentaje":5,"razon":"","usuarioId":"'$USUARIO_MOZO'"}' "$BASE_URL/$PEDIDO_CERVEZA/descuento-manual")
HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
if [ "$HTTP_CODE" -eq 200 ]; then 
    print_result 0 "Aceptó razón vacía (comportamiento esperado)"
else 
    print_result 1 "Falló: rechazó razón vacía (HTTP $HTTP_CODE)"
fi

echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}✓ Suite de Tests Finalizada${NC}"
echo -e "${GREEN}============================================${NC}"