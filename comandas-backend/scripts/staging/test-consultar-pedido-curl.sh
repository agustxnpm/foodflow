#!/bin/bash

# Script de pruebas para HU-06: Consultar Detalle de Pedido
# Endpoint: GET /api/mesas/{mesaId}/pedido-actual

BASE_URL="http://localhost:8080"
ENDPOINT="/api/mesas"

# Colores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  PRUEBAS HU-06: Consultar Pedido      ${NC}"
echo -e "${BLUE}========================================${NC}\n"

# IDs de las mesas de prueba
MESA_10_CON_PEDIDO="b1111111-1111-1111-1111-111111111111"
MESA_5_LIBRE="b2222222-2222-2222-2222-222222222222"
MESA_8_PEDIDO_VACIO="b3333333-3333-3333-3333-333333333333"
MESA_15_UN_ITEM="b4444444-4444-4444-4444-444444444444"
MESA_INEXISTENTE="99999999-9999-9999-9999-999999999999"

sleep 2  # Esperar que la app esté lista

# ============================================
# CASO 1: Mesa con pedido completo (4 ítems)
# ============================================
echo -e "${GREEN}📋 CASO 1: Mesa 10 - Pedido con 4 ítems${NC}"
echo -e "${YELLOW}Esperado: 200 OK con detalle completo del pedido${NC}"
echo -e "Total esperado: \$1,631.50 (700 + 361.50 + 270 + 360)\n"

curl -s -X GET \
  "${BASE_URL}${ENDPOINT}/${MESA_10_CON_PEDIDO}/pedido-actual" \
  -H "Content-Type: application/json" \
  | jq '.'

echo -e "\n${BLUE}────────────────────────────────────────${NC}\n"

# ============================================
# CASO 2: Mesa libre (sin pedido activo)
# ============================================
echo -e "${GREEN}📋 CASO 2: Mesa 5 - Estado LIBRE (sin pedido)${NC}"
echo -e "${YELLOW}Esperado: 500 con mensaje 'no tiene un pedido activo'${NC}\n"

curl -s -X GET \
  "${BASE_URL}${ENDPOINT}/${MESA_5_LIBRE}/pedido-actual" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n" \
  | head -20

echo -e "\n${BLUE}────────────────────────────────────────${NC}\n"

# ============================================
# CASO 3: Mesa con pedido vacío (sin ítems)
# ============================================
echo -e "${GREEN}📋 CASO 3: Mesa 8 - Pedido recién abierto (0 ítems)${NC}"
echo -e "${YELLOW}Esperado: 200 OK con items=[] y total=0.00${NC}\n"

curl -s -X GET \
  "${BASE_URL}${ENDPOINT}/${MESA_8_PEDIDO_VACIO}/pedido-actual" \
  -H "Content-Type: application/json" \
  | jq '.'

echo -e "\n${BLUE}────────────────────────────────────────${NC}\n"

# ============================================
# CASO 4: Mesa con un solo ítem
# ============================================
echo -e "${GREEN}📋 CASO 4: Mesa 15 - Pedido con 1 ítem${NC}"
echo -e "${YELLOW}Esperado: 200 OK con 1 ítem, total=\$250.00${NC}\n"

curl -s -X GET \
  "${BASE_URL}${ENDPOINT}/${MESA_15_UN_ITEM}/pedido-actual" \
  -H "Content-Type: application/json" \
  | jq '.'

echo -e "\n${BLUE}────────────────────────────────────────${NC}\n"

# ============================================
# CASO 5: Mesa inexistente
# ============================================
echo -e "${GREEN}📋 CASO 5: Mesa inexistente${NC}"
echo -e "${YELLOW}Esperado: 500 con mensaje 'La mesa no existe'${NC}\n"

curl -s -X GET \
  "${BASE_URL}${ENDPOINT}/${MESA_INEXISTENTE}/pedido-actual" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n" \
  | head -20

echo -e "\n${BLUE}────────────────────────────────────────${NC}\n"

# ============================================
# CASO 6: ID inválido (formato incorrecto)
# ============================================
echo -e "${GREEN}📋 CASO 6: ID con formato inválido${NC}"
echo -e "${YELLOW}Esperado: 500 con error de formato UUID${NC}\n"

curl -s -X GET \
  "${BASE_URL}${ENDPOINT}/id-invalido-xyz/pedido-actual" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n" \
  | head -20

echo -e "\n${BLUE}========================================${NC}"
echo -e "${BLUE}  FIN DE PRUEBAS                        ${NC}"
echo -e "${BLUE}========================================${NC}\n"

# Resumen de verificación manual
echo -e "${GREEN}✓ Verificaciones esperadas:${NC}"
echo "1. Mesa 10: Total = 1631.50 (2×350 + 3×120.50 + 6×45 + 2×180)"
echo "2. Mesa 5: Error 'no tiene un pedido activo'"
echo "3. Mesa 8: items=[], totalParcial=0.00"
echo "4. Mesa 15: 1 ítem, total=250.00"
echo "5. Mesa inexistente: Error 'La mesa no existe'"
echo "6. ID inválido: Error de formato UUID"
