#!/bin/bash

# Script de prueba para HU-22: Stock Management
# Endpoint: PATCH /api/productos/{id}/stock
# Fecha: 2026-02-13

BASE_URL="http://localhost:8080/api/productos"
PRODUCTO_ID="11111111-1111-1111-1111-111111111111" # Hamburguesa Clásica

echo "=========================================="
echo "HU-22: Stock Management - Test de Endpoint"
echo "=========================================="
echo ""

echo "1. Consultar producto antes del ajuste:"
echo "----------------------------------------"
curl -s -X GET "${BASE_URL}/${PRODUCTO_ID}" \
  -H "Content-Type: application/json" \
  -H "X-Local-Id: 123e4567-e89b-12d3-a456-426614174000" | jq '.'
echo ""

echo "2. Ajuste de stock: INGRESO_MERCADERIA (+50 unidades):"
echo "-------------------------------------------------------"
curl -s -X PATCH "${BASE_URL}/${PRODUCTO_ID}/stock" \
  -H "Content-Type: application/json" \
  -H "X-Local-Id: 123e4567-e89b-12d3-a456-426614174000" \
  -d '{
    "cantidad": 50,
    "tipo": "INGRESO_MERCADERIA",
    "motivo": "Reposición desde depósito central"
  }' | jq '.'
echo ""

echo "3. Ajuste de stock: AJUSTE_MANUAL (-10 unidades por rotura):"
echo "------------------------------------------------------------"
curl -s -X PATCH "${BASE_URL}/${PRODUCTO_ID}/stock" \
  -H "Content-Type: application/json" \
  -H "X-Local-Id: 123e4567-e89b-12d3-a456-426614174000" \
  -d '{
    "cantidad": -10,
    "tipo": "AJUSTE_MANUAL",
    "motivo": "Mercadería en mal estado descartada"
  }' | jq '.'
echo ""

echo "4. Consultar producto después de los ajustes:"
echo "----------------------------------------------"
curl -s -X GET "${BASE_URL}/${PRODUCTO_ID}" \
  -H "Content-Type: application/json" \
  -H "X-Local-Id: 123e4567-e89b-12d3-a456-426614174000" | jq '.'
echo ""

echo "5. Activar control de stock en el producto:"
echo "--------------------------------------------"
curl -s -X PATCH "${BASE_URL}/${PRODUCTO_ID}/stock" \
  -H "Content-Type: application/json" \
  -H "X-Local-Id: 123e4567-e89b-12d3-a456-426614174000" \
  -d '{
    "cantidad": 100,
    "tipo": "INGRESO_MERCADERIA",
    "motivo": "Stock inicial para activar control"
  }' | jq '.'
echo ""

echo "=========================================="
echo "Test completado"
echo "=========================================="
