#!/bin/bash

# Suite de Tests CURL para ProductoController (HU-19)
# Prueba el CRUD completo + filtrado por color
# Prerequisito: ./ff up && ./ff staging scripts/staging/seed-productos.sql

set -e

BASE_URL="http://localhost:8080/api/productos"
CURL_TIMEOUT=5

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

test_count=0
pass_count=0
fail_count=0

log_test() {
    echo -e "\n${BLUE}[TEST $((test_count + 1))]${NC} $1"
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

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_response() {
    echo "$1" | jq '.' 2>/dev/null || echo "$1"
}

# Banner
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}         Suite de Tests - ProductoController (HU-19)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

# Verificar que el backend esté corriendo
log_info "Verificando conexión con el backend en $BASE_URL..."
health_check=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout $CURL_TIMEOUT "$BASE_URL" 2>/dev/null || echo "000")

if [ "$health_check" = "000" ]; then
    log_error "❌ No se pudo conectar al backend en http://localhost:8080"
    log_error "Asegúrate de ejecutar: ./ff up"
    exit 1
fi

log_pass "Conexión exitosa con el backend\n"

# ============================================================================
# TEST 1: GET /api/productos - Listar todos los productos
# ============================================================================
log_test "Listar todos los productos (sin filtro)"

response=$(curl -s -w "\n%{http_code}" --max-time $CURL_TIMEOUT "$BASE_URL" 2>/dev/null || echo -e "\n000")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    count=$(echo "$body" | jq 'length')
    log_pass "Status 200 OK - Total de productos: $count"
    log_info "Primeros 3 productos:"
    echo "$body" | jq '.[0:3]'
else
    log_fail "Expected 200, got $http_code"
fi

# ============================================================================
# TEST 2: GET /api/productos?color=%23FF0000 - Filtrar por color ROJO
# ============================================================================
log_test "Filtrar productos por color ROJO (#FF0000)"

response=$(curl -s -w "\n%{http_code}" --max-time $CURL_TIMEOUT "$BASE_URL?color=%23FF0000" 2>/dev/null || echo -e "\n000")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    count=$(echo "$body" | jq 'length')
    colores=$(echo "$body" | jq '[.[].colorHex] | unique')
    
    if echo "$colores" | grep -q "FF0000"; then
        log_pass "Status 200 OK - Productos rojos encontrados: $count"
        log_info "Productos filtrados:"
        echo "$body" | jq '.[] | {nombre, precio, colorHex}'
    else
        log_fail "No se encontraron productos con color #FF0000"
    fi
else
    log_fail "Expected 200, got $http_code"
fi

# ============================================================================
# TEST 3: GET /api/productos?color=%2300FF00 - Filtrar por color VERDE
# ============================================================================
log_test "Filtrar productos por color VERDE (#00FF00)"

response=$(curl -s -w "\n%{http_code}" --max-time $CURL_TIMEOUT "$BASE_URL?color=%2300FF00" 2>/dev/null || echo -e "\n000")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    count=$(echo "$body" | jq 'length')
    log_pass "Status 200 OK - Productos verdes encontrados: $count"
    log_info "Nombres:"
    echo "$body" | jq -r '.[].nombre'
else
    log_fail "Expected 200, got $http_code"
fi

# ============================================================================
# TEST 4: POST /api/productos - Crear nuevo producto
# ============================================================================
log_test "Crear nuevo producto (Producto de Prueba)"

nuevo_producto=$(cat <<EOF
{
  "nombre": "Producto Test CRUD",
  "precio": 999.99,
  "activo": true,
  "colorHex": "#AA00AA"
}
EOF
)

response=$(curl -s -w "\n%{http_code}" --max-time $CURL_TIMEOUT -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d "$nuevo_producto" 2>/dev/null || echo -e "\n000")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "201" ]; then
    producto_id=$(echo "$body" | jq -r '.id')
    log_pass "Status 201 CREATED - Producto creado con ID: $producto_id"
    log_info "Producto creado:"
    print_response "$body"
    
    # Guardar ID para tests posteriores
    echo "$producto_id" > /tmp/test_producto_id.txt
else
    log_fail "Expected 201, got $http_code"
    print_response "$body"
fi

# ============================================================================
# TEST 5: POST /api/productos - Intentar crear producto duplicado (409)
# ============================================================================
log_test "Intentar crear producto con nombre duplicado (debe fallar con 409)"

producto_duplicado=$(cat <<EOF
{
  "nombre": "Hamburguesa Clásica",
  "precio": 1500.00,
  "activo": true,
  "colorHex": "#FF0000"
}
EOF
)

response=$(curl -s -w "\n%{http_code}" --max-time $CURL_TIMEOUT -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d "$producto_duplicado" 2>/dev/null || echo -e "\n000")
http_code=$(echo "$response" | tail -n1)

if [ "$http_code" = "500" ]; then
    log_pass "Status 500 (esperado por ahora, será 409 con ControllerAdvice)"
    log_info "Este error es esperado hasta implementar el ControllerAdvice"
else
    log_fail "Expected 500 (temporal), got $http_code"
fi

# ============================================================================
# TEST 6: POST /api/productos - Crear producto con color en minúsculas (normalización)
# ============================================================================
log_test "Crear producto con color en minúsculas (debe normalizarse a mayúsculas)"

producto_lowercase=$(cat <<EOF
{
  "nombre": "Producto Lowercase Color",
  "precio": 555.55,
  "activo": true,
  "colorHex": "#abc123"
}
EOF
)

response=$(curl -s -w "\n%{http_code}" --max-time $CURL_TIMEOUT -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d "$producto_lowercase" 2>/dev/null || echo -e "\n000")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "201" ]; then
    color_normalizado=$(echo "$body" | jq -r '.colorHex')
    if [ "$color_normalizado" = "#ABC123" ]; then
        log_pass "Status 201 - Color normalizado correctamente a mayúsculas: $color_normalizado"
    else
        log_fail "Color no fue normalizado. Esperado: #ABC123, Obtenido: $color_normalizado"
    fi
    
    # Guardar ID para limpieza
    id_lowercase=$(echo "$body" | jq -r '.id')
    echo "$id_lowercase" >> /tmp/test_producto_ids_cleanup.txt
else
    log_fail "Expected 201, got $http_code"
fi

# ============================================================================
# TEST 7: POST /api/productos - Crear producto sin color (debe asignar #FFFFFF)
# ============================================================================
log_test "Crear producto sin especificar color (debe asignar #FFFFFF)"

producto_sin_color=$(cat <<EOF
{
  "nombre": "Producto Sin Color",
  "precio": 777.77,
  "activo": true
}
EOF
)

response=$(curl -s -w "\n%{http_code}" --max-time $CURL_TIMEOUT -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d "$producto_sin_color" 2>/dev/null || echo -e "\n000")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "201" ]; then
    color_default=$(echo "$body" | jq -r '.colorHex')
    if [ "$color_default" = "#FFFFFF" ]; then
        log_pass "Status 201 - Color default asignado correctamente: $color_default"
    else
        log_fail "Color default incorrecto. Esperado: #FFFFFF, Obtenido: $color_default"
    fi
    
    # Guardar ID para limpieza
    id_sin_color=$(echo "$body" | jq -r '.id')
    echo "$id_sin_color" >> /tmp/test_producto_ids_cleanup.txt
else
    log_fail "Expected 201, got $http_code"
fi

# ============================================================================
# TEST 8: PUT /api/productos/{id} - Editar producto existente
# ============================================================================
log_test "Editar producto existente (cambiar nombre y precio)"

if [ -f /tmp/test_producto_id.txt ]; then
    producto_id=$(cat /tmp/test_producto_id.txt)
    
    producto_editado=$(cat <<EOF
{
  "nombre": "Producto Test EDITADO",
  "precio": 1234.56,
  "activo": true,
  "colorHex": "#FF00FF"
}
EOF
)

    response=$(curl -s -w "\n%{http_code}" --max-time $CURL_TIMEOUT -X PUT "$BASE_URL/$producto_id" \
      -H "Content-Type: application/json" \
      -d "$producto_editado" 2>/dev/null || echo -e "\n000")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        nuevo_nombre=$(echo "$body" | jq -r '.nombre')
        nuevo_precio=$(echo "$body" | jq -r '.precio')
        
        if [ "$nuevo_nombre" = "Producto Test EDITADO" ] && [ "$nuevo_precio" = "1234.56" ]; then
            log_pass "Status 200 OK - Producto editado correctamente"
            log_info "Datos actualizados:"
            print_response "$body"
        else
            log_fail "Los datos no se actualizaron correctamente"
        fi
    else
        log_fail "Expected 200, got $http_code"
    fi
else
    log_fail "No se pudo obtener el ID del producto creado en TEST 4"
fi

# ============================================================================
# TEST 9: PUT /api/productos/{id} - Editar desactivando producto
# ============================================================================
log_test "Editar producto cambiando estado a INACTIVO"

if [ -f /tmp/test_producto_id.txt ]; then
    producto_id=$(cat /tmp/test_producto_id.txt)
    
    producto_inactivo=$(cat <<EOF
{
  "nombre": "Producto Test EDITADO",
  "precio": 1234.56,
  "activo": false,
  "colorHex": "#FF00FF"
}
EOF
)

    response=$(curl -s -w "\n%{http_code}" --max-time $CURL_TIMEOUT -X PUT "$BASE_URL/$producto_id" \
      -H "Content-Type: application/json" \
      -d "$producto_inactivo" 2>/dev/null || echo -e "\n000")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        estado=$(echo "$body" | jq -r '.activo')
        
        if [ "$estado" = "false" ]; then
            log_pass "Status 200 OK - Producto desactivado correctamente"
        else
            log_fail "El estado no se actualizó a inactivo"
        fi
    else
        log_fail "Expected 200, got $http_code"
    fi
fi

# ============================================================================
# TEST 10: PUT /api/productos/{id-inexistente} - Editar producto que no existe
# ============================================================================
log_test "Intentar editar producto inexistente (debe fallar)"

id_inexistente="00000000-0000-0000-0000-000000000000"

producto_inexistente=$(cat <<EOF
{
  "nombre": "No Existe",
  "precio": 1.00,
  "activo": true,
  "colorHex": "#000000"
}
EOF
)

response=$(curl -s -w "\n%{http_code}" --max-time $CURL_TIMEOUT -X PUT "$BASE_URL/$id_inexistente" \
  -H "Content-Type: application/json" \
  -d "$producto_inexistente" 2>/dev/null || echo -e "\n000")
http_code=$(echo "$response" | tail -n1)

if [ "$http_code" = "500" ]; then
    log_pass "Status 500 (esperado por ahora, será 404 con ControllerAdvice)"
    log_info "Este error es esperado hasta implementar el ControllerAdvice"
else
    log_fail "Expected 500 (temporal), got $http_code"
fi

# ============================================================================
# TEST 11: DELETE /api/productos/{id} - Eliminar producto
# ============================================================================
log_test "Eliminar producto creado en TEST 4"

if [ -f /tmp/test_producto_id.txt ]; then
    producto_id=$(cat /tmp/test_producto_id.txt)
    
    response=$(curl -s -w "\n%{http_code}" --max-time $CURL_TIMEOUT -X DELETE "$BASE_URL/$producto_id" 2>/dev/null || echo -e "\n000")
    http_code=$(echo "$response" | tail -n1)

    if [ "$http_code" = "204" ]; then
        log_pass "Status 204 NO CONTENT - Producto eliminado correctamente"
        
        # Verificar que ya no existe
        response_verificacion=$(curl -s -w "\n%{http_code}" --max-time $CURL_TIMEOUT "$BASE_URL" 2>/dev/null || echo -e "\n000")
        body_verificacion=$(echo "$response_verificacion" | sed '$d')
        existe=$(echo "$body_verificacion" | jq --arg id "$producto_id" 'any(.id == $id)')
        
        if [ "$existe" = "false" ]; then
            log_info "Verificación: El producto ya no aparece en el listado"
        else
            log_fail "Verificación: El producto aún aparece en el listado"
        fi
    else
        log_fail "Expected 204, got $http_code"
    fi
fi

# ============================================================================
# TEST 12: DELETE /api/productos/{id-inexistente} - Eliminar producto que no existe
# ============================================================================
log_test "Intentar eliminar producto inexistente (debe fallar)"

id_inexistente="00000000-0000-0000-0000-000000000000"

response=$(curl -s -w "\n%{http_code}" --max-time $CURL_TIMEOUT -X DELETE "$BASE_URL/$id_inexistente" 2>/dev/null || echo -e "\n000")
http_code=$(echo "$response" | tail -n1)

if [ "$http_code" = "500" ]; then
    log_pass "Status 500 (esperado por ahora, será 404 con ControllerAdvice)"
    log_info "Este error es esperado hasta implementar el ControllerAdvice"
else
    log_fail "Expected 500 (temporal), got $http_code"
fi

# ============================================================================
# LIMPIEZA: Eliminar productos de prueba creados
# ============================================================================
log_test "Limpieza de productos de prueba"

if [ -f /tmp/test_producto_ids_cleanup.txt ]; then
    while read -r id; do
        curl -s --max-time $CURL_TIMEOUT -X DELETE "$BASE_URL/$id" > /dev/null 2>&1
    done < /tmp/test_producto_ids_cleanup.txt
    
    rm /tmp/test_producto_ids_cleanup.txt
    log_info "Productos de prueba eliminados"
fi

[ -f /tmp/test_producto_id.txt ] && rm /tmp/test_producto_id.txt

# ============================================================================
# RESUMEN
# ============================================================================
echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}                         RESUMEN DE TESTS${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "Total de tests ejecutados: ${BLUE}$test_count${NC}"
echo -e "Tests exitosos: ${GREEN}$pass_count${NC}"
echo -e "Tests fallidos: ${RED}$fail_count${NC}"

if [ $fail_count -eq 0 ]; then
    echo -e "\n${GREEN}✓ Todos los tests pasaron exitosamente!${NC}\n"
    exit 0
else
    echo -e "\n${RED}✗ Algunos tests fallaron${NC}\n"
    exit 1
fi
