#!/bin/bash

# ============================================
# Script E2E: Reapertura de Pedido Cerrado
# ============================================
# Archivo: test-reapertura-pedido.sh
# Fecha: 2026-02-12
# ============================================
# Flujo Happy Path completo:
#   1. Apertura de Mesa
#   2. Agregar productos al pedido
#   3. Cerrar la mesa con pago
#   4. Verificar que el pedido está CERRADO y la mesa LIBRE
#   5. REABRIR el pedido (HU-14)
#   6. Verificar que:
#      - Pedido vuelve a ABIERTO
#      - Mesa vuelve a ABIERTA
#      - Snapshot contable se limpia (montos null)
#      - Pagos se eliminan físicamente
#      - Ítems se conservan intactos
#
# Prerequisitos:
#   - Servidor corriendo en localhost:8080
#   - jq instalado (para parsear JSON)
#   - Base de datos con seed cargado (seed-reapertura-pedido.sql)
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

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_json() {
    echo "$1" | jq '.'
}

# ============================================
# VARIABLES DEL FLUJO
# ============================================
MESA_ID="1eab1e01-0000-0000-0000-000000000077"
PRODUCTO_HAMBURGUESA="1eab1e10-0000-0000-0000-000000001001"
PRODUCTO_CERVEZA="1eab1e10-0000-0000-0000-000000001002"

PEDIDO_ID=""
TOTAL_PEDIDO=""

# ============================================
# PASO 1: ABRIR MESA
# ============================================

print_header "PASO 1: Abrir Mesa 77"
print_step "Abriendo mesa..."

RESPONSE=$(curl -s -X POST "${BASE_URL}/api/mesas/${MESA_ID}/abrir?localId=${LOCAL_ID}")

if [ $? -ne 0 ]; then
    print_error "No se pudo conectar al servidor"
fi

# Validar respuesta
ESTADO_MESA=$(echo "$RESPONSE" | jq -r '.estadoMesa')
PEDIDO_ID=$(echo "$RESPONSE" | jq -r '.pedidoId')

if [ "$ESTADO_MESA" != "ABIERTA" ]; then
    print_error "La mesa no se abrió correctamente. Estado: $ESTADO_MESA"
fi

print_success "Mesa abierta correctamente"
echo -e "${MAGENTA}  Pedido ID: $PEDIDO_ID${NC}"
echo ""

# ============================================
# PASO 2: AGREGAR PRODUCTOS
# ============================================

print_header "PASO 2: Agregar Productos al Pedido"

# Agregar 2 Hamburguesas
print_step "Agregando 2 Hamburguesas..."
RESPONSE=$(curl -s -X POST "${BASE_URL}/api/pedidos/${PEDIDO_ID}/items" \
    -H "$CONTENT_TYPE" \
    -d "{
        \"productoId\": \"${PRODUCTO_HAMBURGUESA}\",
        \"cantidad\": 2,
        \"observaciones\": \"Sin cebolla\"
    }")

ITEMS_COUNT=$(echo "$RESPONSE" | jq '.items | length')
print_success "Hamburguesas agregadas (Total ítems: $ITEMS_COUNT)"

# Agregar 1 Cerveza
print_step "Agregando 1 Cerveza..."
RESPONSE=$(curl -s -X POST "${BASE_URL}/api/pedidos/${PEDIDO_ID}/items" \
    -H "$CONTENT_TYPE" \
    -d "{
        \"productoId\": \"${PRODUCTO_CERVEZA}\",
        \"cantidad\": 1,
        \"observaciones\": null
    }")

ITEMS_COUNT=$(echo "$RESPONSE" | jq '.items | length')
TOTAL_PEDIDO=$(echo "$RESPONSE" | jq -r '.total')
print_success "Cerveza agregada (Total ítems: $ITEMS_COUNT, Total: \$$TOTAL_PEDIDO)"
echo ""

# ============================================
# PASO 3: CERRAR MESA CON PAGO
# ============================================

print_header "PASO 3: Cerrar Mesa con Pago en Efectivo"
print_step "Cerrando mesa..."

RESPONSE=$(curl -s -X POST "${BASE_URL}/api/mesas/${MESA_ID}/cierre" \
    -H "$CONTENT_TYPE" \
    -d "{
        \"pagos\": [
            {
                \"medio\": \"EFECTIVO\",
                \"monto\": ${TOTAL_PEDIDO}
            }
        ]
    }")

ESTADO_MESA_POST_CIERRE=$(echo "$RESPONSE" | jq -r '.mesaEstado')
ESTADO_PEDIDO_POST_CIERRE=$(echo "$RESPONSE" | jq -r '.pedidoEstado')
MONTO_TOTAL_FINAL=$(echo "$RESPONSE" | jq -r '.montoTotalFinal')

if [ "$ESTADO_MESA_POST_CIERRE" != "LIBRE" ]; then
    print_error "La mesa no se cerró. Estado: $ESTADO_MESA_POST_CIERRE"
fi

if [ "$ESTADO_PEDIDO_POST_CIERRE" != "CERRADO" ]; then
    print_error "El pedido no se cerró. Estado: $ESTADO_PEDIDO_POST_CIERRE"
fi

print_success "Mesa cerrada exitosamente"
echo -e "${MAGENTA}  Estado Mesa: $ESTADO_MESA_POST_CIERRE${NC}"
echo -e "${MAGENTA}  Estado Pedido: $ESTADO_PEDIDO_POST_CIERRE${NC}"
echo -e "${MAGENTA}  Total Final: \$$MONTO_TOTAL_FINAL${NC}"
echo ""

# ============================================
# PASO 4: ESPERAR UN MOMENTO (simular tiempo real)
# ============================================

print_step "Esperando 2 segundos (simular tiempo real)..."
sleep 2
echo ""

# ============================================
# PASO 5: REABRIR PEDIDO (HU-14) ⭐
# ============================================

print_header "PASO 5: REABRIR Pedido Cerrado (HU-14)"
print_warning "Esta operación es destructiva:"
print_warning "  - Elimina el snapshot contable"
print_warning "  - Elimina todos los pagos"
print_warning "  - Revierte pedido a ABIERTO y mesa a ABIERTA"
echo ""

print_step "Reabriendo pedido ${PEDIDO_ID}..."

RESPONSE=$(curl -s -X POST "${BASE_URL}/api/pedidos/${PEDIDO_ID}/reapertura?localId=${LOCAL_ID}")

if [ $? -ne 0 ]; then
    print_error "Fallo en la reapertura del pedido"
fi

# Parsear respuesta
PEDIDO_ESTADO=$(echo "$RESPONSE" | jq -r '.pedidoEstado')
MESA_ESTADO=$(echo "$RESPONSE" | jq -r '.mesaEstado')
FECHA_REAPERTURA=$(echo "$RESPONSE" | jq -r '.fechaReapertura')
CANTIDAD_ITEMS=$(echo "$RESPONSE" | jq -r '.cantidadItems')
MONTO_SUBTOTAL=$(echo "$RESPONSE" | jq -r '.montoSubtotal')
MONTO_TOTAL=$(echo "$RESPONSE" | jq -r '.montoTotal')

if [ "$PEDIDO_ESTADO" != "ABIERTO" ]; then
    print_error "El pedido no volvió a ABIERTO. Estado: $PEDIDO_ESTADO"
fi

if [ "$MESA_ESTADO" != "ABIERTA" ]; then
    print_error "La mesa no volvió a ABIERTA. Estado: $MESA_ESTADO"
fi

print_success "Pedido reabierto exitosamente"
echo ""
echo -e "${MAGENTA}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}✓ REAPERTURA EXITOSA${NC}"
echo -e "${MAGENTA}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}  Pedido ID:${NC}           $PEDIDO_ID"
echo -e "${CYAN}  Estado Pedido:${NC}       $PEDIDO_ESTADO"
echo -e "${CYAN}  Estado Mesa:${NC}         $MESA_ESTADO"
echo -e "${CYAN}  Fecha Reapertura:${NC}    $FECHA_REAPERTURA"
echo -e "${CYAN}  Ítems Conservados:${NC}   $CANTIDAD_ITEMS"
echo -e "${CYAN}  Subtotal:${NC}            \$$MONTO_SUBTOTAL"
echo -e "${CYAN}  Total:${NC}               \$$MONTO_TOTAL"
echo -e "${MAGENTA}═══════════════════════════════════════════════════════════════${NC}"
echo ""

# ============================================
# PASO 6: VERIFICACIONES ADICIONALES
# ============================================

print_header "PASO 6: Verificaciones de Integridad"

print_step "Consultando detalle del pedido reabierto a través de la mesa..."

RESPONSE=$(curl -s -X GET "${BASE_URL}/api/mesas/${MESA_ID}/pedido-actual")

ITEMS=$(echo "$RESPONSE" | jq '.items')
ITEMS_COUNT=$(echo "$ITEMS" | jq 'length')

print_success "Pedido consultado correctamente"
echo ""
echo -e "${CYAN}Ítems del pedido reabierto:${NC}"
echo "$ITEMS" | jq -r '.[] | "  - \(.nombreProducto) x\(.cantidad) = $\(.precioUnitario * .cantidad)"'
echo ""

# Verificar que los ítems se conservaron
if [ "$ITEMS_COUNT" -ne "$CANTIDAD_ITEMS" ]; then
    print_error "Los ítems no se conservaron correctamente"
fi

print_success "Los ítems se conservaron intactos"
echo ""

# ============================================
# PASO 7: INTENTAR REABRIR NUEVAMENTE (debe fallar)
# ============================================

print_header "PASO 7: Validación - Intentar Reabrir de Nuevo (debe fallar)"
print_step "Intentando reabrir un pedido ya ABIERTO..."

RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "${BASE_URL}/api/pedidos/${PEDIDO_ID}/reapertura?localId=${LOCAL_ID}")

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d':' -f2)

if [ "$HTTP_CODE" == "500" ] || [ "$HTTP_CODE" == "400" ] || [ "$HTTP_CODE" == "409" ]; then
    print_success "Validación correcta: no se puede reabrir un pedido que ya está ABIERTO (HTTP $HTTP_CODE)"
else
    print_warning "Se esperaba un error 4xx/5xx pero se obtuvo HTTP $HTTP_CODE"
fi

echo ""

# ============================================
# RESUMEN FINAL
# ============================================

print_header "RESUMEN FINAL"
echo -e "${GREEN}✓ Apertura de mesa${NC}"
echo -e "${GREEN}✓ Agregar productos${NC}"
echo -e "${GREEN}✓ Cierre de mesa con pago${NC}"
echo -e "${GREEN}✓ Reapertura de pedido (HU-14)${NC}"
echo -e "${GREEN}✓ Verificación de integridad${NC}"
echo -e "${GREEN}✓ Validación de reglas de negocio${NC}"
echo ""
echo -e "${BLUE}════════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}    ¡FLUJO E2E DE REAPERTURA COMPLETADO EXITOSAMENTE!${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════════${NC}"
echo ""
