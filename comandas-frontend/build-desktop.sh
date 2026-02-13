#!/bin/bash

echo "==================================="
echo "FoodFlow - Build Desktop App"
echo "==================================="
echo ""

# 1. Compilar backend
echo "1/3 Compilando backend Spring Boot..."
cd ../comandas-backend
./mvnw clean package -DskipTests

if [ $? -ne 0 ]; then
  echo "❌ Error compilando backend"
  exit 1
fi

# 2. Copiar JAR
echo ""
echo "2/3 Copiando JAR al proyecto Tauri..."
cp target/comandas-backend-0.0.1-SNAPSHOT.jar \
   ../comandas-frontend/src-tauri/binaries/backend.jar

if [ $? -ne 0 ]; then
  echo "❌ Error copiando JAR"
  exit 1
fi

# 3. Construir app desktop
echo ""
echo "3/3 Construyendo aplicación desktop..."
cd ../comandas-frontend
npm run tauri:build

if [ $? -ne 0 ]; then
  echo "❌ Error construyendo app desktop"
  exit 1
fi

echo ""
echo "==================================="
echo "✅ Build completado exitosamente"
echo "==================================="
echo ""
echo "Instalador generado en:"
echo "src-tauri/target/release/bundle/"
echo ""
