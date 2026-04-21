#!/usr/bin/env bash
# =================================================================
# test-all-endpoints.sh — Smoke test de TODOS los endpoints REST
#
# Uso: ./test-all-endpoints.sh [BASE_URL]
#       Default: http://localhost:8080
#
# Requisitos:
#   - Backend corriendo con profile offline
#   - Base de datos vacía (el script activa el sistema)
#   - curl + jq instalados
#
# El script se ejecuta sobre una DB limpia:
#   1. Activa identidad (POST /api/config/identity)
#   2. Ejecuta tests por controller
#   3. Limpia la DB al finalizar (éxito o fallo)
# =================================================================
set -uo pipefail

BASE="${1:-http://localhost:8080}"
H="Content-Type: application/json"
DB_PATH="${FOODFLOW_DB_PATH:-comandas.db}"

# ── Colores y contadores ─────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; NC='\033[0m'; BOLD='\033[1m'
PASS=0; FAIL=0; TOTAL=0
FAILURES=""

# Archivo temporal reutilizado por assert/extract
RESP_FILE=$(mktemp)
trap 'rm -f "$RESP_FILE"' EXIT

# ── Helpers ───────────────────────────────────────────────────────

# assert <test_name> <expected_http_code> <curl_args...>
assert() {
  local name="$1" expected="$2"; shift 2
  TOTAL=$((TOTAL + 1))

  local status
  status=$(curl -s -o "$RESP_FILE" -w "%{http_code}" "$@") || status="000"

  if [[ "$status" == "$expected" ]]; then
    printf "  ${GREEN}✔${NC} %-62s ${GREEN}%s${NC}\n" "$name" "$status"
    PASS=$((PASS + 1))
  else
    printf "  ${RED}✘${NC} %-62s ${RED}%s${NC} (esperado ${YELLOW}%s${NC})\n" "$name" "$status" "$expected"
    local body; body=$(head -c 300 "$RESP_FILE" 2>/dev/null)
    [[ -n "$body" ]] && printf "    ${RED}→ %s${NC}\n" "$body"
    FAIL=$((FAIL + 1))
    FAILURES="${FAILURES}\n    ✘ $name → $status (esperado $expected)"
  fi
}

# json_field <jq_filter> — extrae un campo del último RESP_FILE
json_field() {
  jq -r "$1" "$RESP_FILE" 2>/dev/null
}

section() {
  echo ""
  printf "${CYAN}${BOLD}═══ %s ═══${NC}\n" "$1"
}

# ── Verificar DB limpia ──────────────────────────────────────────
if [[ -f "$DB_PATH" ]]; then
  printf "\n${YELLOW}⚠  DB existente: %s — se eliminará al finalizar${NC}\n" "$DB_PATH"
fi

LOCAL_ID="11111111-1111-1111-1111-111111111111"
USER_ID="99999999-9999-9999-9999-999999999999"
TODAY=$(date +%Y-%m-%d)

# =================================================================
#  1. HealthController
# =================================================================
section "HealthController"

assert "GET /api/health" 200 "$BASE/api/health"

# =================================================================
#  2. IdentityController
# =================================================================
section "IdentityController"

assert "GET  identity (sin config)"      200 "$BASE/api/config/identity"

assert "POST identity (activar)"         201 \
  -X POST -H "$H" "$BASE/api/config/identity" \
  -d "{\"localId\":\"$LOCAL_ID\",\"nombreLocal\":\"Test Resto\",\"direccion\":\"Calle 123\",\"telefono\":\"1234567890\"}"

assert "POST identity (idempotente)"     200 \
  -X POST -H "$H" "$BASE/api/config/identity" \
  -d "{\"localId\":\"$LOCAL_ID\",\"nombreLocal\":\"Test Resto\"}"

assert "POST identity (otro UUID → 409)" 409 \
  -X POST -H "$H" "$BASE/api/config/identity" \
  -d '{"localId":"22222222-2222-2222-2222-222222222222","nombreLocal":"Otro"}'

assert "GET  identity (configurado)"     200 "$BASE/api/config/identity"

assert "PUT  identity (editar)"          200 \
  -X PUT -H "$H" "$BASE/api/config/identity" \
  -d '{"nombreLocal":"Resto Editado","direccion":"Real 456","telefono":"9876543210"}'

# =================================================================
#  3. ConfiguracionController
# =================================================================
section "ConfiguracionController"

assert "GET  impresoras"                 200 "$BASE/api/config/impresoras"
assert "GET  impresora-predeterminada"   200 "$BASE/api/config/impresora-predeterminada"
assert "POST impresora-predeterminada"   200 \
  -X POST -H "$H" "$BASE/api/config/impresora-predeterminada" \
  -d '{"nombre":"EPSON TM-T20III"}'

# =================================================================
#  4. CategoriaController
# =================================================================
section "CategoriaController"

assert "GET  categorias (vacío)"         200 "$BASE/api/categorias"

assert "POST categorias (crear)"         201 \
  -X POST -H "$H" "$BASE/api/categorias" \
  -d '{"nombre":"Bebidas","colorHex":"#FF0000"}'
CAT_ID=$(json_field '.id')

assert "PUT  categorias (editar)"        200 \
  -X PUT -H "$H" "$BASE/api/categorias/$CAT_ID" \
  -d '{"nombre":"Bebidas Frías","colorHex":"#0000FF"}'

assert "GET  categorias (con datos)"     200 "$BASE/api/categorias"

# =================================================================
#  5. ProductoController
# =================================================================
section "ProductoController"

assert "GET  productos (vacío)"          200 "$BASE/api/productos"

assert "POST productos (crear 1)"       201 \
  -X POST -H "$H" "$BASE/api/productos" \
  -d "{\"nombre\":\"Cerveza IPA\",\"precio\":2500.00,\"activo\":true,\"colorHex\":\"#FFD700\",\"controlaStock\":true,\"categoriaId\":\"$CAT_ID\"}"
PROD_ID=$(json_field '.id')

assert "POST productos (crear 2)"       201 \
  -X POST -H "$H" "$BASE/api/productos" \
  -d '{"nombre":"Hamburguesa","precio":3500.00,"activo":true,"colorHex":"#8B4513"}'
PROD2_ID=$(json_field '.id')

assert "GET  productos/{id}"             200 "$BASE/api/productos/$PROD_ID"
assert "GET  productos?activo=true"      200 "$BASE/api/productos?activo=true"

assert "PUT  productos (editar)"         200 \
  -X PUT -H "$H" "$BASE/api/productos/$PROD_ID" \
  -d "{\"nombre\":\"Cerveza IPA Ed\",\"precio\":2800.00,\"activo\":true,\"colorHex\":\"#FFD700\",\"controlaStock\":true,\"categoriaId\":\"$CAT_ID\"}"

assert "PATCH productos/{id}/stock"      200 \
  -X PATCH -H "$H" "$BASE/api/productos/$PROD_ID/stock" \
  -d '{"cantidad":50,"tipo":"INGRESO_MERCADERIA","motivo":"Stock inicial"}'

assert "GET  productos/{id}/variantes"   200 "$BASE/api/productos/$PROD_ID/variantes"

assert "POST productos/{id}/variantes"   201 \
  -X POST -H "$H" "$BASE/api/productos/$PROD_ID/variantes" \
  -d "{\"nombre\":\"Cerveza Pinta\",\"precio\":3200.00,\"cantidadDiscosCarne\":2,\"activo\":true,\"categoriaId\":\"$CAT_ID\"}"

# =================================================================
#  6. PromocionController
# =================================================================
section "PromocionController"

assert "GET  promociones (vacío)"        200 "$BASE/api/promociones"

assert "POST promociones (crear)"        201 \
  -X POST -H "$H" "$BASE/api/promociones" \
  -d '{
    "nombre":"Happy Hour","descripcion":"Descuento","prioridad":10,
    "tipoEstrategia":"DESCUENTO_DIRECTO",
    "descuentoDirecto":{"modo":"PORCENTAJE","valor":20},
    "triggers":[{"tipo":"TEMPORAL","fechaDesde":"2026-01-01","fechaHasta":"2026-12-31"}]
  }'
PROMO_ID=$(json_field '.id')

assert "GET  promociones/{id}"           200 "$BASE/api/promociones/$PROMO_ID"
assert "GET  promociones?estado=ACTIVA"  200 "$BASE/api/promociones?estado=ACTIVA"

assert "PUT  promociones (editar)"       200 \
  -X PUT -H "$H" "$BASE/api/promociones/$PROMO_ID" \
  -d '{
    "nombre":"Happy Hour Ed","descripcion":"Edit","prioridad":5,
    "tipoEstrategia":"DESCUENTO_DIRECTO",
    "descuentoDirecto":{"modo":"PORCENTAJE","valor":15},
    "triggers":[{"tipo":"TEMPORAL","fechaDesde":"2026-01-01","fechaHasta":"2026-12-31"}]
  }'

assert "PUT  promociones/{id}/alcance"   200 \
  -X PUT -H "$H" "$BASE/api/promociones/$PROMO_ID/alcance" \
  -d "{\"items\":[{\"referenciaId\":\"$PROD_ID\",\"tipo\":\"PRODUCTO\",\"rol\":\"TARGET\"}]}"

assert "PATCH estado → INACTIVA"         200 \
  -X PATCH -H "$H" "$BASE/api/promociones/$PROMO_ID/estado" \
  -d '{"estado":"INACTIVA"}'

assert "PATCH estado → ACTIVA"           200 \
  -X PATCH -H "$H" "$BASE/api/promociones/$PROMO_ID/estado" \
  -d '{"estado":"ACTIVA"}'

assert "DELETE promociones/{id}"         204 \
  -X DELETE "$BASE/api/promociones/$PROMO_ID"

# =================================================================
#  7. CajaController (abrir jornada)
# =================================================================
section "CajaController"

assert "GET  caja/estado"                200 "$BASE/api/caja/estado"

assert "POST caja/abrir"                 201 \
  -X POST -H "$H" "$BASE/api/caja/abrir" \
  -d '{"montoInicial":10000}'

assert "POST caja/abrir (dupla → 409)"   409 \
  -X POST -H "$H" "$BASE/api/caja/abrir" \
  -d '{"montoInicial":5000}'

assert "POST caja/egresos"               201 \
  -X POST -H "$H" "$BASE/api/caja/egresos" \
  -d '{"monto":500,"descripcion":"Insumos"}'

assert "POST caja/ingresos"              201 \
  -X POST -H "$H" "$BASE/api/caja/ingresos" \
  -d '{"monto":1500,"descripcion":"Delivery"}'

assert "GET  caja/reporte"               200 "$BASE/api/caja/reporte?fecha=$TODAY"
assert "GET  caja/reportes/productos"    200 "$BASE/api/caja/reportes/productos?fecha=$TODAY"
assert "GET  caja/jornadas"              200 "$BASE/api/caja/jornadas?desde=$TODAY&hasta=$TODAY"

# =================================================================
#  8. MesaController
# =================================================================
section "MesaController"

assert "GET  mesas"                      200 "$BASE/api/mesas"

# Crear mesa extra para DELETE test
assert "POST mesas (crear mesa 2)"       201 \
  -X POST -H "$H" "$BASE/api/mesas" \
  -d '{"numero":2}'
MESA2_ID=$(json_field '.id')

# Abrir mesa 1 (creada por seed)
MESA1_ID=$(curl -s "$BASE/api/mesas" | jq -r '.[0].id')

assert "POST mesas/{id}/abrir"           201 \
  -X POST -H "$H" "$BASE/api/mesas/$MESA1_ID/abrir"
PEDIDO_ID=$(json_field '.pedidoId')

assert "GET  mesas/{id}/pedido-actual"   200 "$BASE/api/mesas/$MESA1_ID/pedido-actual"
assert "GET  mesas/{id}/ticket"          200 "$BASE/api/mesas/$MESA1_ID/ticket"
assert "GET  mesas/{id}/comanda"         200 "$BASE/api/mesas/$MESA1_ID/comanda"

# =================================================================
#  9. PedidoController
# =================================================================
section "PedidoController"

# Agregar item 1
assert "POST pedidos/{id}/items (1)"     200 \
  -X POST -H "$H" "$BASE/api/pedidos/$PEDIDO_ID/items" \
  -d "{\"productoId\":\"$PROD_ID\",\"cantidad\":2,\"observaciones\":\"Sin espuma\"}"
ITEM_ID=$(json_field '.items[0].itemId')

# Agregar item 2
assert "POST pedidos/{id}/items (2)"     200 \
  -X POST -H "$H" "$BASE/api/pedidos/$PEDIDO_ID/items" \
  -d "{\"productoId\":\"$PROD2_ID\",\"cantidad\":1}"
ITEM2_ID=$(json_field '.items[-1].itemId')

# Modificar cantidad
assert "PATCH items/{id} (cantidad)"     200 \
  -X PATCH -H "$H" "$BASE/api/pedidos/$PEDIDO_ID/items/$ITEM_ID" \
  -d '{"cantidad":3}'

# Descuento manual por item
assert "POST descuento-manual item"      200 \
  -X POST -H "$H" "$BASE/api/pedidos/$PEDIDO_ID/items/$ITEM_ID/descuento-manual" \
  -d "{\"tipoDescuento\":\"PORCENTAJE\",\"valor\":10,\"razon\":\"Test\",\"usuarioId\":\"$USER_ID\"}"

# Descuento manual global
assert "POST descuento-manual global"    200 \
  -X POST -H "$H" "$BASE/api/pedidos/$PEDIDO_ID/descuento-manual" \
  -d "{\"tipoDescuento\":\"PORCENTAJE\",\"valor\":5,\"razon\":\"Global\",\"usuarioId\":\"$USER_ID\"}"

# Eliminar item 2
assert "DELETE items/{id}"               200 \
  -X DELETE "$BASE/api/pedidos/$PEDIDO_ID/items/$ITEM2_ID"

# Enviar comanda a cocina
assert "POST enviar-cocina"              200 \
  -X POST "$BASE/api/mesas/$MESA1_ID/enviar-cocina?soloNuevos=true"

# Imprimir ticket
assert "POST imprimir-ticket"            200 \
  -X POST "$BASE/api/mesas/$MESA1_ID/imprimir-ticket"

# Obtener total del pedido para cierre exacto
curl -s "$BASE/api/mesas/$MESA1_ID/pedido-actual" > "$RESP_FILE"
TOTAL_PEDIDO=$(json_field '.totalParcial')

# Cerrar mesa
assert "POST mesas/{id}/cierre"          200 \
  -X POST -H "$H" "$BASE/api/mesas/$MESA1_ID/cierre" \
  -d "{\"pagos\":[{\"medio\":\"EFECTIVO\",\"monto\":$TOTAL_PEDIDO}]}"

# Consultar pedido cerrado (caja)
assert "GET  caja/pedidos/{id}"          200 "$BASE/api/caja/pedidos/$PEDIDO_ID"

# Corregir pedido cerrado (sin cambiar cantidades, solo re-confirmar pagos)
assert "PUT  caja/pedidos/{id}/correccion" 200 \
  -X PUT -H "$H" "$BASE/api/caja/pedidos/$PEDIDO_ID/correccion" \
  -d "{\"items\":[{\"itemId\":\"$ITEM_ID\",\"cantidad\":3}],\"pagos\":[{\"medio\":\"EFECTIVO\",\"monto\":$TOTAL_PEDIDO}]}"

# Reapertura
assert "POST reapertura"                 200 \
  -X POST "$BASE/api/pedidos/$PEDIDO_ID/reapertura"

# Re-cerrar (recalcular total después de reapertura)
curl -s "$BASE/api/mesas/$MESA1_ID/pedido-actual" > "$RESP_FILE"
TOTAL_REAP=$(json_field '.totalParcial')
assert "POST cierre post-reapertura"     200 \
  -X POST -H "$H" "$BASE/api/mesas/$MESA1_ID/cierre" \
  -d "{\"pagos\":[{\"medio\":\"TARJETA\",\"monto\":$TOTAL_REAP}]}"

# =================================================================
#  10. Cierre de jornada + PDF
# =================================================================
section "CajaController (cierre)"

# Eliminar mesa 2 (libre)
assert "DELETE mesas/{id}"               204 \
  -X DELETE "$BASE/api/mesas/$MESA2_ID"

assert "POST cierre-jornada"             200 \
  -X POST "$BASE/api/caja/cierre-jornada"

# Obtener jornada para PDF
curl -s "$BASE/api/caja/jornadas?desde=$TODAY&hasta=$TODAY" > "$RESP_FILE"
JORNADA_ID=$(json_field '.[0].id')

if [[ -n "$JORNADA_ID" && "$JORNADA_ID" != "null" ]]; then
  assert "GET  jornada reporte-pdf"      200 "$BASE/api/caja/jornadas/$JORNADA_ID/reporte-pdf"
fi

# =================================================================
#  Limpieza de catálogo
# =================================================================
section "Limpieza catálogo"

assert "DELETE producto 1"               204 -X DELETE "$BASE/api/productos/$PROD_ID"
assert "DELETE producto 2"               204 -X DELETE "$BASE/api/productos/$PROD2_ID"
assert "DELETE categoría"                204 -X DELETE "$BASE/api/categorias/$CAT_ID"

# =================================================================
#  Cleanup DB
# =================================================================
section "CLEANUP"
if [[ -f "$DB_PATH" ]]; then
  rm -f "$DB_PATH"
  printf "  ${YELLOW}⚑${NC} DB eliminada: %s\n" "$DB_PATH"
else
  printf "  ${YELLOW}⚑${NC} DB no en %s\n" "$DB_PATH"
fi

# =================================================================
#  RESUMEN
# =================================================================
echo ""
printf "${BOLD}═══════════════════════════════════════════════════════════════${NC}\n"
printf "${BOLD}  RESULTADOS: ${GREEN}%d passed${NC}  ${RED}%d failed${NC}  / %d total\n" "$PASS" "$FAIL" "$TOTAL"

if [[ $FAIL -gt 0 ]]; then
  printf "\n${BOLD}  Fallos:${NC}"
  printf "%b\n" "$FAILURES"
fi
printf "${BOLD}═══════════════════════════════════════════════════════════════${NC}\n"

exit $FAIL
