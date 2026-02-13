#!/bin/bash

# ============================================
# Suite de Tests CURL para CajaController
# Archivo: test-caja.sh
# Fecha: 2026-02-12
# ============================================
# Prerequisitos:
#   1. Servidor corriendo en localhost:8080
#   2. Base de datos cargada: ./ff staging scripts/staging/test-caja-data.sql

set -e

BASE_URL="http://localhost:8080/api/caja"
CURL_TIMEOUT=5

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

test_count=0
pass_count=0
fail_count=0

log_test() {
    echo -e "\n${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║ TEST #$((test_count + 1)): $1${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
    ((++test_count))
}

log_pass() {
    echo -e "${GREEN}✓ PASS${NC} $1"
    ((++pass_count))
}

log_fail() {
    echo -e "${RED}✗ FAIL${NC} $1"
    ((++fail_count))
}

log_info() {
    echo -e "${YELLOW}ℹ${NC} $1"
}

log_response() {
    echo -e "${MAGENTA}Response:${NC}"
    echo "$1" | jq '.' 2>/dev/null || echo "$1"
}

# Validar que jq está instalado
if ! command -v jq &> /dev/null; then
    echo -e "${RED}ERROR: jq no está instalado. Instalalo con: sudo apt-get install jq${NC}"
    exit 1
fi

# Verificar que el servidor está levantado
echo -e "${BLUE}Verificando conectividad con el servidor...${NC}"
if ! curl -s -o /dev/null -w "%{http_code}" --max-time $CURL_TIMEOUT http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo -e "${RED}ERROR: El servidor no responde en localhost:8080${NC}"
    echo "Ejecuta: ./ff up"
    exit 1
fi
echo -e "${GREEN}✓ Servidor disponible${NC}\n"

# ============================================
# VARIABLES COMPARTIDAS
# ============================================

FECHA_HOY=$(date +%Y-%m-%d)
EGRESO_ID_1=""
EGRESO_ID_2=""

echo -e "${YELLOW}════════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}   SUITE DE TESTS: CONTROL DE CAJA${NC}"
echo -e "${YELLOW}   Fecha de prueba: $FECHA_HOY${NC}"
echo -e "${YELLOW}════════════════════════════════════════════════════════════════${NC}"

# ============================================
# TEST 1: Obtener Reporte de Caja Sin Egresos
# ============================================

log_test "Obtener reporte de caja del día (sin egresos previos)"

log_info "GET /api/caja/reporte?fecha=$FECHA_HOY"

response=$(curl -s -X GET \
  "${BASE_URL}/reporte?fecha=${FECHA_HOY}" \
  -H "Content-Type: application/json" \
  --max-time $CURL_TIMEOUT)

log_response "$response"

# Validaciones
ventas_reales=$(echo "$response" | jq -r '.totalVentasReales')
consumo_interno=$(echo "$response" | jq -r '.totalConsumoInterno')
balance_efectivo=$(echo "$response" | jq -r '.balanceEfectivo')
total_egresos=$(echo "$response" | jq -r '.totalEgresos')

# Pedido 1: EFECTIVO $2100 (venta real)
# Pedido 2: TARJETA $1800 (venta real)
# Pedido 3: A_CUENTA $2400 (consumo interno)

if [[ "$ventas_reales" == "3900.00" ]]; then
    log_pass "Total ventas reales correcto: $3900.00"
else
    log_fail "Total ventas reales incorrecto. Expected: 3900.00, Got: $ventas_reales"
fi

if [[ "$consumo_interno" == "2400.00" ]]; then
    log_pass "Total consumo interno correcto: $2400.00"
else
    log_fail "Total consumo interno incorrecto. Expected: 2400.00, Got: $consumo_interno"
fi

if [[ "$balance_efectivo" == "2100.00" ]]; then
    log_pass "Balance efectivo correcto (sin egresos): $2100.00"
else
    log_fail "Balance efectivo incorrecto. Expected: 2100.00, Got: $balance_efectivo"
fi

if [[ "$total_egresos" == "0.00" || "$total_egresos" == "0" ]]; then
    log_pass "Total egresos inicial correcto: $0.00"
else
    log_fail "Total egresos incorrecto. Expected: 0.00, Got: $total_egresos"
fi

# ============================================
# TEST 2: Registrar Egreso - Compra de Insumos
# ============================================

log_test "Registrar egreso de caja (compra de insumos)"

PAYLOAD_EGRESO_1=$(cat <<EOF
{
  "monto": 850.50,
  "descripcion": "Compra de café en grano - Proveedor ABC"
}
EOF
)

log_info "POST /api/caja/egresos"
echo -e "${MAGENTA}Payload:${NC}"
echo "$PAYLOAD_EGRESO_1" | jq '.'

http_response=$(curl -s -w "\n%{http_code}" -X POST \
  "${BASE_URL}/egresos" \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD_EGRESO_1" \
  --max-time $CURL_TIMEOUT)

http_status=$(echo "$http_response" | tail -n1)
response=$(echo "$http_response" | sed '$d')

log_response "$response"

# Validaciones
if [[ "$http_status" == "201" ]]; then
    log_pass "Status HTTP 201 CREATED correcto"
else
    log_fail "Status HTTP incorrecto. Expected: 201, Got: $http_status"
fi

EGRESO_ID_1=$(echo "$response" | jq -r '.id')
numero_comprobante=$(echo "$response" | jq -r '.numeroComprobante')
monto=$(echo "$response" | jq -r '.monto')
descripcion=$(echo "$response" | jq -r '.descripcion')

if [[ -n "$EGRESO_ID_1" && "$EGRESO_ID_1" != "null" ]]; then
    log_pass "ID de movimiento generado: $EGRESO_ID_1"
else
    log_fail "ID de movimiento no generado"
fi

if [[ "$numero_comprobante" =~ ^EGR-[0-9]{8}-[0-9]{6}-[A-F0-9]{4}$ ]]; then
    log_pass "Número de comprobante válido: $numero_comprobante"
else
    log_fail "Formato de comprobante inválido: $numero_comprobante (esperado: EGR-YYYYMMDD-HHmmss-XXXX)"
fi

if [[ "$monto" == "850.50" ]]; then
    log_pass "Monto correcto: $850.50"
else
    log_fail "Monto incorrecto. Expected: 850.50, Got: $monto"
fi

if [[ "$descripcion" == "Compra de café en grano - Proveedor ABC" ]]; then
    log_pass "Descripción correcta"
else
    log_fail "Descripción incorrecta"
fi

# ============================================
# TEST 3: Registrar Segundo Egreso - Pago Servicio
# ============================================

log_test "Registrar segundo egreso (pago de servicio)"

PAYLOAD_EGRESO_2=$(cat <<EOF
{
  "monto": 450.00,
  "descripcion": "Pago gas del local - Metrogas"
}
EOF
)

log_info "POST /api/caja/egresos"
echo -e "${MAGENTA}Payload:${NC}"
echo "$PAYLOAD_EGRESO_2" | jq '.'

response=$(curl -s -X POST \
  "${BASE_URL}/egresos" \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD_EGRESO_2" \
  --max-time $CURL_TIMEOUT)

log_response "$response"

EGRESO_ID_2=$(echo "$response" | jq -r '.id')
numero_comprobante_2=$(echo "$response" | jq -r '.numeroComprobante')

if [[ -n "$EGRESO_ID_2" && "$EGRESO_ID_2" != "null" ]]; then
    log_pass "Segundo egreso registrado: $EGRESO_ID_2"
else
    log_fail "Segundo egreso no registrado"
fi

if [[ "$numero_comprobante_2" =~ ^EGR-[0-9]{8}-[0-9]{6}-[A-F0-9]{4}$ ]]; then
    log_pass "Comprobante del segundo egreso válido: $numero_comprobante_2"
else
    log_fail "Formato de comprobante inválido: $numero_comprobante_2 (esperado: EGR-YYYYMMDD-HHmmss-XXXX)"
fi

# ============================================
# TEST 4: Validar Números de Comprobante Únicos
# ============================================

log_test "Validar que los números de comprobante son únicos"

if [[ "$numero_comprobante" != "$numero_comprobante_2" ]]; then
    log_pass "Los números de comprobante son únicos"
    log_info "Comprobante 1: $numero_comprobante"
    log_info "Comprobante 2: $numero_comprobante_2"
else
    log_fail "Los comprobantes son idénticos (deben ser únicos)"
fi

# ============================================
# TEST 5: Reporte de Caja Actualizado con Egresos
# ============================================

log_test "Obtener reporte actualizado con egresos registrados"

log_info "GET /api/caja/reporte?fecha=$FECHA_HOY"

response=$(curl -s -X GET \
  "${BASE_URL}/reporte?fecha=${FECHA_HOY}" \
  -H "Content-Type: application/json" \
  --max-time $CURL_TIMEOUT)

log_response "$response"

# Validaciones
total_egresos=$(echo "$response" | jq -r '.totalEgresos')
balance_efectivo=$(echo "$response" | jq -r '.balanceEfectivo')
cant_egresos=$(echo "$response" | jq -r '.movimientos | length')

expected_egresos="1300.50" # 850.50 + 450.00
expected_balance="799.50"  # 2100.00 - 1300.50

if [[ "$total_egresos" == "$expected_egresos" ]]; then
    log_pass "Total egresos acumulados correcto: $expected_egresos"
else
    log_fail "Total egresos incorrecto. Expected: $expected_egresos, Got: $total_egresos"
fi

if [[ "$balance_efectivo" == "$expected_balance" ]]; then
    log_pass "Balance efectivo final correcto: $expected_balance (entrada $2100 - egresos $1300.50)"
else
    log_fail "Balance efectivo incorrecto. Expected: $expected_balance, Got: $balance_efectivo"
fi

if [[ "$cant_egresos" == "2" ]]; then
    log_pass "Cantidad de egresos en reporte correcta: 2"
else
    log_fail "Cantidad de egresos incorrecta. Expected: 2, Got: $cant_egresos"
fi

# ============================================
# TEST 6: Validar Desglose por Medio de Pago
# ============================================

log_test "Validar desglose de ventas por medio de pago"

log_info "Analizando campo 'desglosePorMedioPago' del reporte anterior"

pago_efectivo=$(echo "$response" | jq -r '.desglosePorMedioPago.EFECTIVO // 0')
pago_tarjeta=$(echo "$response" | jq -r '.desglosePorMedioPago.TARJETA // 0')
pago_a_cuenta=$(echo "$response" | jq -r '.desglosePorMedioPago.A_CUENTA // 0')

if [[ "$pago_efectivo" == "2100.00" ]]; then
    log_pass "Ventas en EFECTIVO correctas: $2100.00"
else
    log_fail "Ventas en EFECTIVO incorrectas. Expected: 2100.00, Got: $pago_efectivo"
fi

if [[ "$pago_tarjeta" == "1800.00" ]]; then
    log_pass "Ventas en TARJETA correctas: $1800.00"
else
    log_fail "Ventas en TARJETA incorrectas. Expected: 1800.00, Got: $pago_tarjeta"
fi

if [[ "$pago_a_cuenta" == "2400.00" ]]; then
    log_pass "Consumo A_CUENTA correcto: $2400.00"
else
    log_fail "Consumo A_CUENTA incorrecto. Expected: 2400.00, Got: $pago_a_cuenta"
fi

# ============================================
# TEST 7: Validación de Datos del Egreso en Reporte
# ============================================

log_test "Validar datos completos de egresos en el reporte"

egreso_1_monto=$(echo "$response" | jq -r '.movimientos[0].monto')
egreso_1_desc=$(echo "$response" | jq -r '.movimientos[0].descripcion')
egreso_1_comprobante=$(echo "$response" | jq -r '.movimientos[0].numeroComprobante')

egreso_2_monto=$(echo "$response" | jq -r '.movimientos[1].monto')
egreso_2_desc=$(echo "$response" | jq -r '.movimientos[1].descripcion')

# El orden puede variar por fecha, validamos que ambos están presentes
total_montos=$(echo "$egreso_1_monto + $egreso_2_monto" | bc)

if [[ "$total_montos" == "$expected_egresos" ]]; then
    log_pass "Montos de egresos individuales suman correctamente: $total_montos"
else
    log_fail "Suma de montos incorrecta. Expected: $expected_egresos, Got: $total_montos"
fi

if [[ -n "$egreso_1_comprobante" && "$egreso_1_comprobante" != "null" ]]; then
    log_pass "Comprobante incluido en reporte: $egreso_1_comprobante"
else
    log_fail "Comprobante no incluido en reporte"
fi

descripciones="$egreso_1_desc | $egreso_2_desc"
if [[ "$descripciones" == *"café en grano"* && "$descripciones" == *"gas del local"* ]]; then
    log_pass "Descripciones de egresos presentes en reporte"
else
    log_fail "Descripciones incompletas en reporte"
fi

# ============================================
# TEST 8: Intentar Registrar Egreso con Monto Negativo (Validación)
# ============================================

log_test "Validar rechazo de egreso con monto negativo"

PAYLOAD_INVALIDO=$(cat <<EOF
{
  "monto": -100.00,
  "descripcion": "Egreso inválido con monto negativo"
}
EOF
)

log_info "POST /api/caja/egresos (con monto negativo)"
echo -e "${MAGENTA}Payload:${NC}"
echo "$PAYLOAD_INVALIDO" | jq '.'

http_status=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
  "${BASE_URL}/egresos" \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD_INVALIDO" \
  --max-time $CURL_TIMEOUT)

if [[ "$http_status" == "400" || "$http_status" == "422" ]]; then
    log_pass "Solicitud rechazada correctamente (HTTP $http_status)"
else
    log_fail "Se esperaba rechazo HTTP 400/422, se obtuvo: $http_status"
fi

# ============================================
# TEST 9: Intentar Registrar Egreso con Monto Cero (Validación)
# ============================================

log_test "Validar rechazo de egreso con monto cero"

PAYLOAD_CERO=$(cat <<EOF
{
  "monto": 0.00,
  "descripcion": "Egreso inválido con monto cero"
}
EOF
)

log_info "POST /api/caja/egresos (con monto cero)"

http_status=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
  "${BASE_URL}/egresos" \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD_CERO" \
  --max-time $CURL_TIMEOUT)

if [[ "$http_status" == "400" || "$http_status" == "422" ]]; then
    log_pass "Solicitud rechazada correctamente (HTTP $http_status)"
else
    log_fail "Se esperaba rechazo HTTP 400/422, se obtuvo: $http_status"
fi

# ============================================
# TEST 10: Reporte de Día Sin Datos
# ============================================

log_test "Obtener reporte de un día sin movimientos"

FECHA_FUTURA="2026-12-31"
log_info "GET /api/caja/reporte?fecha=$FECHA_FUTURA"

response=$(curl -s -X GET \
  "${BASE_URL}/reporte?fecha=${FECHA_FUTURA}" \
  -H "Content-Type: application/json" \
  --max-time $CURL_TIMEOUT)

log_response "$response"

ventas_reales=$(echo "$response" | jq -r '.totalVentasReales')
total_egresos=$(echo "$response" | jq -r '.totalEgresos')
balance=$(echo "$response" | jq -r '.balanceEfectivo')

if [[ ("$ventas_reales" == "0.00" || "$ventas_reales" == "0") && ("$total_egresos" == "0.00" || "$total_egresos" == "0") && ("$balance" == "0.00" || "$balance" == "0") ]]; then
    log_pass "Reporte de día vacío devuelve valores en cero correctamente"
else
    log_fail "Reporte de día vacío contiene valores inesperados"
    log_info "Ventas: $ventas_reales | Egresos: $total_egresos | Balance: $balance"
fi

# ============================================
# RESUMEN FINAL
# ============================================

echo -e "\n${YELLOW}════════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}   RESUMEN DE EJECUCIÓN${NC}"
echo -e "${YELLOW}════════════════════════════════════════════════════════════════${NC}"
echo -e "Total tests ejecutados: ${BLUE}$test_count${NC}"
echo -e "Tests exitosos:         ${GREEN}$pass_count${NC}"
echo -e "Tests fallidos:         ${RED}$fail_count${NC}"

if [[ $fail_count -eq 0 ]]; then
    echo -e "\n${GREEN}✓✓✓ TODOS LOS TESTS PASARON EXITOSAMENTE ✓✓✓${NC}\n"
    exit 0
else
    echo -e "\n${RED}✗✗✗ ALGUNOS TESTS FALLARON ✗✗✗${NC}\n"
    exit 1
fi
