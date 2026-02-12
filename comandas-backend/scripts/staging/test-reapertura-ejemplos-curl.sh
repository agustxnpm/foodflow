#!/bin/bash

# ============================================
# Script de Prueba Simple: Reapertura de Pedido
# ============================================
# Casos de prueba independientes con curl
# Prerequisitos: Proyecto corriendo + datos cargados
# ============================================

BASE_URL="http://localhost:8080"
LOCAL_ID="123e4567-e89b-12d3-a456-426614174000"

# Colores
GREEN='\033[0;32m'
RED='\033[0;31m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo ""
echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   EJEMPLOS DE CURL - REAPERTURA DE PEDIDO (HU-14)         ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# ============================================
# CASO 1: Reapertura exitosa de pedido cerrado
# ============================================

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}CASO 1: Reapertura exitosa de pedido cerrado${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "Prerequisitos:"
echo "  - Pedido existente en estado CERRADO"
echo "  - Mesa en estado LIBRE"
echo ""
echo "Comando:"
echo -e "${YELLOW}curl -X POST '${BASE_URL}/api/pedidos/{PEDIDO_ID}/reapertura?localId=${LOCAL_ID}' \\
  -H 'Content-Type: application/json' \\
  -v${NC}"
echo ""
echo "Ejemplo con IDs reales:"
PEDIDO_ID="1eab1e99-aaaa-bbbb-cccc-dddddddddddd"
echo -e "${YELLOW}curl -X POST '${BASE_URL}/api/pedidos/${PEDIDO_ID}/reapertura?localId=${LOCAL_ID}' \\
  -H 'Content-Type: application/json' | jq '.'${NC}"
echo ""
echo "Respuesta esperada (200 OK):"
cat <<'EOF'
{
  "pedidoId": "1eab1e99-aaaa-bbbb-cccc-dddddddddddd",
  "pedidoEstado": "ABIERTO",
  "mesaId": "1eab1e01-0000-0000-0000-000000000077",
  "mesaEstado": "ABIERTA",
  "fechaReapertura": "2026-02-12T15:30:45",
  "cantidadItems": 2,
  "montoSubtotal": 6800.00,
  "montoTotal": 6800.00
}
EOF
echo ""
echo ""

# ============================================
# CASO 2: Error - Intentar reabrir pedido ABIERTO
# ============================================

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${RED}CASO 2: Error - Intentar reabrir pedido ABIERTO${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "Validación: Solo se pueden reabrir pedidos en estado CERRADO"
echo ""
echo "Comando:"
echo -e "${YELLOW}curl -X POST '${BASE_URL}/api/pedidos/{PEDIDO_ABIERTO_ID}/reapertura?localId=${LOCAL_ID}' \\
  -H 'Content-Type: application/json' \\
  -v${NC}"
echo ""
echo "Respuesta esperada (500 Internal Server Error):"
cat <<'EOF'
{
  "timestamp": "2026-02-12T15:31:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Solo se pueden reabrir pedidos en estado CERRADO. Estado actual: ABIERTO",
  "path": "/api/pedidos/xxx/reapertura"
}
EOF
echo ""
echo ""

# ============================================
# CASO 3: Error - Pedido no pertenece al local (Multi-tenancy)
# ============================================

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${RED}CASO 3: Error - Pedido de otro local (Multi-tenancy)${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "Validación: El pedido debe pertenecer al local especificado"
echo ""
LOCAL_OTRO="aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
echo "Comando (con LOCAL_ID incorrecto):"
echo -e "${YELLOW}curl -X POST '${BASE_URL}/api/pedidos/${PEDIDO_ID}/reapertura?localId=${LOCAL_OTRO}' \\
  -H 'Content-Type: application/json' \\
  -v${NC}"
echo ""
echo "Respuesta esperada (500 o 400):"
cat <<'EOF'
{
  "timestamp": "2026-02-12T15:32:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "El pedido xxx no pertenece al local yyy",
  "path": "/api/pedidos/xxx/reapertura"
}
EOF
echo ""
echo ""

# ============================================
# CASO 4: Flujo completo automatizado
# ============================================

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}CASO 4: Script completo (E2E)${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "Para ejecutar el flujo completo E2E:"
echo -e "${YELLOW}bash scripts/staging/test-reapertura-pedido.sh${NC}"
echo ""
echo "El script incluye:"
echo "  1. Cargar seed (productos y mesa)"
echo "  2. Abrir mesa"
echo "  3. Agregar productos"
echo "  4. Cerrar mesa con pago"
echo "  5. Reabrir pedido"
echo "  6. Verificar integridad de datos"
echo "  7. Validar reglas de negocio"
echo ""
echo ""

# ============================================
# VERIFICACIÓN DE ESTADO POST-REAPERTURA
# ============================================

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}CONSULTA: Verificar estado post-reapertura${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "Obtener detalle del pedido reabierto a través de la mesa:"
MESA_ID="1eab1e01-0000-0000-0000-000000000077"
echo -e "${YELLOW}curl -X GET '${BASE_URL}/api/mesas/${MESA_ID}/pedido-actual' | jq '.'${NC}"
echo ""
echo "Obtener estado de la mesa:"
echo -e "${YELLOW}curl -X GET '${BASE_URL}/api/mesas?localId=${LOCAL_ID}' | jq '.[] | select(.numero == 77)'${NC}"
echo ""
echo ""

# ============================================
# CONSIDERACIONES TÉCNICAS
# ============================================

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}⚠  CONSIDERACIONES TÉCNICAS${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "La reapertura es una operación DESTRUCTIVA que:"
echo ""
echo "  ${RED}✗${NC} Elimina el snapshot contable (montos finales → null)"
echo "  ${RED}✗${NC} Elimina TODOS los pagos registrados (DELETE físico)"
echo "  ${GREEN}✓${NC} Conserva los ítems del pedido intactos"
echo "  ${GREEN}✓${NC} Revierte pedido a ABIERTO y mesa a ABIERTA"
echo "  ${GREEN}✓${NC} Operación transaccional (pedido + mesa atomicos)"
echo ""
echo "Casos de uso válidos:"
echo "  - Error en el medio de pago (EFECTIVO vs TARJETA)"
echo "  - Mesa cerrada por equivocación"
echo "  - Falta agregar ítems antes del cierre de caja"
echo ""
echo "Restricciones:"
echo "  - Solo antes del cierre de caja definitivo"
echo "  - No permite reabrir pedidos ya facturados (futuro)"
echo ""
echo ""

echo -e "${GREEN}════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  ¡EJEMPLOS CARGADOS! Modifica los IDs según tu contexto${NC}"
echo -e "${GREEN}════════════════════════════════════════════════════════════${NC}"
echo ""
