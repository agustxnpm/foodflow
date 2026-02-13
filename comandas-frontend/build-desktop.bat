@echo off
echo ===================================
echo FoodFlow - Build Desktop App
echo ===================================
echo.

REM 1. Compilar backend
echo 1/3 Compilando backend Spring Boot...
cd ..\comandas-backend
call mvnw.cmd clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
  echo Error compilando backend
  exit /b 1
)

REM 2. Copiar JAR
echo.
echo 2/3 Copiando JAR al proyecto Tauri...
copy target\comandas-backend-0.0.1-SNAPSHOT.jar ^
     ..\comandas-frontend\src-tauri\binaries\backend.jar

if %ERRORLEVEL% NEQ 0 (
  echo Error copiando JAR
  exit /b 1
)

REM 3. Construir app desktop
echo.
echo 3/3 Construyendo aplicacion desktop...
cd ..\comandas-frontend
call npm run tauri:build

if %ERRORLEVEL% NEQ 0 (
  echo Error construyendo app desktop
  exit /b 1
)

echo.
echo ===================================
echo Build completado exitosamente
echo ===================================
echo.
echo Instalador generado en:
echo src-tauri\target\release\bundle\
echo.
pause
