#!/bin/bash
set -e

echo "==================================="
echo "FoodFlow - Build Desktop App"
echo "==================================="
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/../comandas-backend"
FRONTEND_DIR="$SCRIPT_DIR"
TAURI_DIR="$FRONTEND_DIR/src-tauri"
JRE_DIR="$TAURI_DIR/jre"

# 1. Compilar backend
echo "1/4 Compilando backend Spring Boot..."
cd "$BACKEND_DIR"
./mvnw clean package -DskipTests

# 2. Copiar JAR
echo ""
echo "2/4 Copiando JAR al proyecto Tauri..."
cp target/comandas-backend-0.0.1-SNAPSHOT.jar \
   "$TAURI_DIR/binaries/backend.jar"

# 3. Generar JRE mínimo con jlink
echo ""
echo "3/4 Generando JRE embebido con jlink..."
if ! command -v jlink &>/dev/null; then
  echo "❌ jlink no encontrado. Necesitás un JDK 21 completo (no solo JRE)."
  echo "   Instalalo con: sudo apt install openjdk-21-jdk"
  exit 1
fi

# Limpiar JRE anterior si existe
rm -rf "$JRE_DIR"

# Módulos mínimos para Spring Boot + JPA + Web
jlink \
  --add-modules java.base,java.logging,java.sql,java.naming,java.management,java.instrument,java.desktop,java.security.jgss,java.net.http,jdk.unsupported,jdk.crypto.ec \
  --strip-debug \
  --no-man-pages \
  --no-header-files \
  --compress=zip-6 \
  --output "$JRE_DIR"

echo "   JRE generado en: $JRE_DIR ($(du -sh "$JRE_DIR" | cut -f1))"

# 4. Construir app desktop (inyectar jre/** como resource adicional)
echo ""
echo "4/4 Construyendo aplicación desktop..."
cd "$FRONTEND_DIR"

# Inyectar jre/** como recurso adicional para el bundle de producción
export TAURI_CONFIG='{"bundle":{"resources":["binaries/*","jre/**"]}}'
npm run tauri:build

echo ""
echo "==================================="
echo "✅ Build completado exitosamente"
echo "==================================="
echo ""
echo "Instalador generado en:"
echo "src-tauri/target/release/bundle/"
echo ""
