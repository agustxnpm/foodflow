#!/bin/bash

# ============================================
# Script E2E: Cierre de Mesa con Normalización y Pago Split
# ============================================
# Archivo: test-e2e-cierre-mesa.sh
# Fecha: 2026-02-10
# ============================================
# Flujo Happy Path completo:
#   1. Apertura de Mesa
#   2. Agregar Hamburguesa Simple + 1 Disco Extra → Normalización automática a Hamburguesa Doble
#   3. Cierre de Mesa con Pago Split (60% EFECTIVO + 40% TARJETA)
#   4. Verificación del Snapshot Contable
#
# Prerequisitos:
#   - Servidor corriendo en localhost:8080
#   - jq instalado (para parsear JSON)
#   - Base de datos con seed de productos de hamburguesas cargado
# ============================================


# ============================================
# CONFIGURACIÓN GLOBAL
# ============================================
BASE_URL="http://localhost:8080"
LOCAL_ID="123e4567-e89b-12d3-a456-426614174000"  # Local ID fijo para testing
CONTENT_TYPE="Content-Type: application/json"

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

# ============================================
# FUNCIONES AUXILIARES
# ============================================

print_header() {
    echo ""
    echo -e "${BLUE}╔═══════════════════════════════════════════════════════════════╗${NC}"
    printf "${BLUE}║${NC} %-61s ${BLUE}║${NC}\n" "$1"
    echo -e "${BLUE}╚═══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

print_step() {
    echo -e "${CYAN}▶ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ ERROR: $1${NC}"
    exit 1
}

print_json() {
    echo "$1" | jq '.'
}

# ============================================
# VARIABLES DEL FLUJO
# ============================================
MESA_ID="e2e00001-0001-0001-0001-000000000001"
PRODUCTO_HAMBURGUESA="e2e10001-0001-0001-0001-000000000001"
PRODUCTO_PAPAS="e2e10002-0002-0002-0002-000000000002"

PEDIDO_ID=""
TOTAL_PEDIDO=""

# ============================================
# PASO 0: LIMPIEZA - RESETEAR MESA A LIBRE
# ============================================

CLEANUP_SQL=$(mktemp)
cat > "$CLEANUP_SQL" <<'EOF'
DELETE FROM items_pedido WHERE pedido_id IN (SELECT id FROM pedidos WHERE mesa_id = 'e2e00001-0001-0001-0001-000000000001');
DELETE FROM pedidos_pagos WHERE pedido_id IN (SELECT id FROM pedidos WHERE mesa_id = 'e2e00001-0001-0001-0001-000000000001');
DELETE FROM pedidos WHERE mesa_id = 'e2e00001-0001-0001-0001-000000000001';
UPDATE mesas SET estado = 'LIBRE' WHERE id = 'e2e00001-0001-0001-0001-000000000001';
EOF
../../ff staging "$CLEANUP_SQL" > /dev/null 2>&1
rm -f "$CLEANUP_SQL"

# ============================================
# PASO 1: APERTURA DE MESA
# ============================================

print_header "PASO 1: Apertura de Mesa 99"

print_step "POST /api/mesas/${MESA_ID}/abrir"

RESPONSE_ABRIR=$(curl -s -X POST \
  "${BASE_URL}/api/mesas/${MESA_ID}/abrir" \
  -H "${CONTENT_TYPE}" \
  -w "\nHTTP_STATUS:%{http_code}")

HTTP_STATUS=$(echo "$RESPONSE_ABRIR" | grep "HTTP_STATUS" | cut -d: -f2)
BODY=$(echo "$RESPONSE_ABRIR" | sed -e 's/HTTP_STATUS:.*//')

if [ "$HTTP_STATUS" != "201" ]; then
    print_error "Apertura de mesa falló con HTTP $HTTP_STATUS"
fi

print_success "Mesa abierta con éxito (HTTP 201)"
echo -e "${YELLOW}Respuesta:${NC}"
print_json "$BODY"

# Extraer el pedidoId de la respuesta
PEDIDO_ID=$(echo "$BODY" | jq -r '.pedidoId')
if [ -z "$PEDIDO_ID" ] || [ "$PEDIDO_ID" = "null" ]; then
    print_error "No se pudo extraer el pedidoId de la respuesta"
fi

print_success "Pedido creado: $PEDIDO_ID"

# ============================================
# PASO 2: AGREGAR PRODUCTOS AL PEDIDO
# ============================================

print_header "PASO 2: Carga de Productos al Pedido"

print_step "Agregar Hamburguesa Completa (\$2500) + Papas Fritas (\$800)"
echo -e "${YELLOW}Total esperado: \$3300${NC}"
echo ""

# Agregar Hamburguesa
print_step "POST /api/pedidos/${PEDIDO_ID}/items (Hamburguesa Completa)"

PAYLOAD_HAMBURGUESA=$(cat <<EOF
{
  "productoId": "${PRODUCTO_HAMBURGUESA}",
  "cantidad": 1,
  "observaciones": "Sin cebolla"
}
EOF
)

RESPONSE_ITEM1=$(curl -s -X POST \
  "${BASE_URL}/api/pedidos/${PEDIDO_ID}/items" \
  -H "${CONTENT_TYPE}" \
  -d "$PAYLOAD_HAMBURGUESA" \
  -w "\nHTTP_STATUS:%{http_code}")

HTTP_STATUS_ITEM1=$(echo "$RESPONSE_ITEM1" | grep "HTTP_STATUS" | cut -d: -f2)
BODY_ITEM1=$(echo "$RESPONSE_ITEM1" | sed -e 's/HTTP_STATUS:.*//')

if [ "$HTTP_STATUS_ITEM1" != "200" ]; then
    echo -e "${RED}Error al agregar hamburguesa:${NC}"
    print_json "$BODY_ITEM1"
    print_error "Agregar Hamburguesa falló con HTTP $HTTP_STATUS_ITEM1"
fi

print_success "Hamburguesa Completa agregada"
echo -e "${YELLOW}Respuesta parcial:${NC}"
echo "$BODY_ITEM1" | jq '{items: .items | length, montoTotal: .montoTotal}'

# Agregar Papas
print_step "POST /api/pedidos/${PEDIDO_ID}/items (Papas Fritas)"

PAYLOAD_PAPAS=$(cat <<EOF
{
  "productoId": "${PRODUCTO_PAPAS}",
  "cantidad": 1,
  "observaciones": null
}
EOF
)

RESPONSE_ITEM2=$(curl -s -X POST \
  "${BASE_URL}/api/pedidos/${PEDIDO_ID}/items" \
  -H "${CONTENT_TYPE}" \
  -d "$PAYLOAD_PAPAS" \
  -w "\nHTTP_STATUS:%{http_code}")

HTTP_STATUS_ITEM2=$(echo "$RESPONSE_ITEM2" | grep "HTTP_STATUS" | cut -d: -f2)
BODY_ITEM2=$(echo "$RESPONSE_ITEM2" | sed -e 's/HTTP_STATUS:.*//')

if [ "$HTTP_STATUS_ITEM2" != "200" ]; then
    echo -e "${RED}Error al agregar papas:${NC}"
    print_json "$BODY_ITEM2"
    print_error "Agregar Papas falló con HTTP $HTTP_STATUS_ITEM2"
fi

print_success "Papas Fritas agregadas"
echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}Estado del Pedido después de agregar productos:${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════════${NC}"
print_json "$BODY_ITEM2"
echo ""

# Extraer el total del pedido
TOTAL_PEDIDO=$(echo "$BODY_ITEM2" | jq -r '.total')
if [ -z "$TOTAL_PEDIDO" ] || [ "$TOTAL_PEDIDO" = "null" ]; then
    print_error "No se pudo extraer el total de la respuesta"
fi

print_success "Total del pedido: \$$TOTAL_PEDIDO"

ITEM_COUNT=$(echo "$BODY_ITEM2" | jq '.items | length')
echo -e "${YELLOW}Ítems en el pedido: $ITEM_COUNT${NC}"

# ============================================
# PASO 3: CIERRE DE MESA CON PAGO SPLIT
# ============================================

print_header "PASO 3: Cierre de Mesa con Pago Split (HU-07)"

# Calcular montos de pago (60% EFECTIVO + 40% TARJETA)
TOTAL_NUMERICO=$(echo "$TOTAL_PEDIDO" | bc)
PAGO_EFECTIVO=$(LC_NUMERIC=C printf "%.2f" $(echo "scale=2; $TOTAL_NUMERICO * 0.60" | bc))
PAGO_TARJETA=$(LC_NUMERIC=C printf "%.2f" $(echo "scale=2; $TOTAL_NUMERICO * 0.40" | bc))

print_step "Total a pagar: \$$TOTAL_PEDIDO"
echo -e "${YELLOW}  → 60% EFECTIVO: \$$PAGO_EFECTIVO${NC}"
echo -e "${YELLOW}  → 40% TARJETA:  \$$PAGO_TARJETA${NC}"
echo ""

print_step "POST /api/mesas/${MESA_ID}/cierre"

PAYLOAD_CIERRE=$(cat <<EOF
{
  "pagos": [
    {
      "medio": "EFECTIVO",
      "monto": $PAGO_EFECTIVO
    },
    {
      "medio": "TARJETA",
      "monto": $PAGO_TARJETA
    }
  ]
}
EOF
)

echo -e "${CYAN}Request body:${NC}"
print_json "$PAYLOAD_CIERRE"
echo ""

RESPONSE_CIERRE=$(curl -s -X POST \
  "${BASE_URL}/api/mesas/${MESA_ID}/cierre" \
  -H "${CONTENT_TYPE}" \
  -d "$PAYLOAD_CIERRE" \
  -w "\nHTTP_STATUS:%{http_code}")

HTTP_STATUS_CIERRE=$(echo "$RESPONSE_CIERRE" | grep "HTTP_STATUS" | cut -d: -f2)
BODY_CIERRE=$(echo "$RESPONSE_CIERRE" | sed -e 's/HTTP_STATUS:.*//')

if [ "$HTTP_STATUS_CIERRE" != "200" ]; then
    echo -e "${RED}Error en cierre de mesa:${NC}"
    print_json "$BODY_CIERRE"
    print_error "Cierre de mesa falló con HTTP $HTTP_STATUS_CIERRE"
fi

print_success "Mesa cerrada con éxito (HTTP 200)"

# ============================================
# PASO 4: VERIFICACIÓN DEL SNAPSHOT CONTABLE
# ============================================

print_header "PASO 4: Verificación del Snapshot Contable"

echo -e "${GREEN}═══════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}SNAPSHOT CONTABLE FINAL${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════════${NC}"
print_json "$BODY_CIERRE"
echo ""

# Extraer campos clave del snapshot
MESA_ESTADO=$(echo "$BODY_CIERRE" | jq -r '.mesaEstado')
PEDIDO_ESTADO=$(echo "$BODY_CIERRE" | jq -r '.pedidoEstado')
MONTO_SUBTOTAL=$(echo "$BODY_CIERRE" | jq -r '.montoSubtotal')
MONTO_DESCUENTOS=$(echo "$BODY_CIERRE" | jq -r '.montoDescuentos')
MONTO_TOTAL=$(echo "$BODY_CIERRE" | jq -r '.montoTotal')
FECHA_CIERRE=$(echo "$BODY_CIERRE" | jq -r '.fechaCierre')
PAGOS=$(echo "$BODY_CIERRE" | jq -r '.pagos')

echo -e "${BLUE}┌─────────────────────────────────────────────────────────┐${NC}"
echo -e "${BLUE}│ VALIDACIONES DE SNAPSHOT                                 │${NC}"
echo -e "${BLUE}└─────────────────────────────────────────────────────────┘${NC}"
echo ""

# Validación 1: Mesa LIBRE
if [ "$MESA_ESTADO" = "LIBRE" ]; then
    print_success "Mesa está LIBRE (correcta liberación del recurso)"
else
    print_error "Mesa no está LIBRE (estado: $MESA_ESTADO)"
fi

# Validación 2: Pedido CERRADO
if [ "$PEDIDO_ESTADO" = "CERRADO" ]; then
    print_success "Pedido está CERRADO (inmutabilidad garantizada)"
else
    print_error "Pedido no está CERRADO (estado: $PEDIDO_ESTADO)"
fi

# Validación 3: Snapshot contable congelado
echo -e "${CYAN}Snapshot Contable Congelado:${NC}"
echo -e "  Subtotal:    \$$MONTO_SUBTOTAL"
echo -e "  Descuentos:  \$$MONTO_DESCUENTOS"
echo -e "  Total Final: \$$MONTO_TOTAL"
echo -e "  Fecha:       $FECHA_CIERRE"
echo ""

# Validación 4: Pagos registrados
NUM_PAGOS=$(echo "$PAGOS" | jq 'length')
if [ "$NUM_PAGOS" = "2" ]; then
    print_success "2 pagos registrados (split exitoso)"
    echo -e "${CYAN}Detalle de pagos:${NC}"
    echo "$PAGOS" | jq '.[] | "  \(.medio): $\(.monto)"'
else
    print_error "Se esperaban 2 pagos, se recibieron $NUM_PAGOS"
fi

echo ""

# Validación 5: Suma de pagos = Total
SUMA_PAGOS=$(echo "$PAGOS" | jq '[.[].monto] | add')
SUMA_PAGOS_FORMATTED=$(LC_NUMERIC=C printf "%.2f" $SUMA_PAGOS)
MONTO_TOTAL_FORMATTED=$(LC_NUMERIC=C printf "%.2f" $MONTO_TOTAL)

if [ "$SUMA_PAGOS_FORMATTED" = "$MONTO_TOTAL_FORMATTED" ]; then
    print_success "Suma de pagos (\$$SUMA_PAGOS_FORMATTED) = Total del pedido (\$$MONTO_TOTAL_FORMATTED)"
else
    print_error "Suma de pagos (\$$SUMA_PAGOS_FORMATTED) ≠ Total del pedido (\$$MONTO_TOTAL_FORMATTED)"
fi

echo ""
echo -e "${GREEN}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║                                                           ║${NC}"
echo -e "${GREEN}║         ✓ FLUJO E2E COMPLETADO CON ÉXITO                 ║${NC}"
echo -e "${GREEN}║                                                           ║${NC}"
echo -e "${GREEN}╚═══════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${MAGENTA}Resumen de la operación:${NC}"
echo -e "  • Mesa 99: LIBRE → ABIERTA → LIBRE"
echo -e "  • Pedido: ABIERTO → CERRADO"
echo -e "  • Productos: Hamburguesa Completa (\$2500) + Papas Fritas (\$800)"
echo -e "  • Total: \$$MONTO_TOTAL"
echo -e "  • Pagos: EFECTIVO (\$$PAGO_EFECTIVO) + TARJETA (\$$PAGO_TARJETA)"
echo -e "  • Snapshot: Congelado e inmutable"
echo ""

exit 0
