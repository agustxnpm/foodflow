@echo off
setlocal enabledelayedexpansion

echo ===================================
echo FoodFlow - Build Desktop App
echo ===================================
echo.

set "SCRIPT_DIR=%~dp0"
set "BACKEND_DIR=%SCRIPT_DIR%..\comandas-backend"
set "TAURI_DIR=%SCRIPT_DIR%src-tauri"
set "JRE_DIR=%TAURI_DIR%\jre"

REM 1. Compilar backend
echo 1/4 Compilando backend Spring Boot...
cd /d "%BACKEND_DIR%"
call mvnw.cmd clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
  echo Error compilando backend
  exit /b 1
)

REM 2. Copiar JAR
echo.
echo 2/4 Copiando JAR al proyecto Tauri...
copy /Y target\comandas-backend-0.0.1-SNAPSHOT.jar ^
     "%TAURI_DIR%\binaries\backend.jar"

if %ERRORLEVEL% NEQ 0 (
  echo Error copiando JAR
  exit /b 1
)

REM 3. Generar JRE minimo con jlink
echo.
echo 3/4 Generando JRE embebido con jlink...

where jlink >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
  echo Error: jlink no encontrado. Necesitas un JDK 21 completo.
  echo   Descargalo de: https://adoptium.net/
  exit /b 1
)

REM Limpiar JRE anterior si existe
if exist "%JRE_DIR%" rmdir /s /q "%JRE_DIR%"

jlink ^
  --add-modules java.base,java.logging,java.sql,java.naming,java.management,java.instrument,java.desktop,java.security.jgss,java.net.http,jdk.unsupported,jdk.crypto.ec ^
  --strip-debug ^
  --no-man-pages ^
  --no-header-files ^
  --compress=zip-6 ^
  --output "%JRE_DIR%"

if %ERRORLEVEL% NEQ 0 (
  echo Error generando JRE con jlink
  exit /b 1
)

echo   JRE generado en: %JRE_DIR%

REM 4. Construir app desktop (inyectar jre/** como resource adicional)
echo.
echo 4/4 Construyendo aplicacion desktop...
cd /d "%SCRIPT_DIR%"

set TAURI_CONFIG={"bundle":{"resources":["binaries/*","jre/**"]}}
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
