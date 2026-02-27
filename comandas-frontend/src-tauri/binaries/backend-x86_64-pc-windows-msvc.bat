@echo off
setlocal

REM === Buscar JAR ===
if exist "%~dp0backend.jar" (
    set JAR_PATH=%~dp0backend.jar
) else if exist "%~dp0..\binaries\backend.jar" (
    set JAR_PATH=%~dp0..\binaries\backend.jar
) else (
    echo Error: No se encuentra backend.jar
    exit /b 1
)

REM === Buscar Java embebido (JRE custom via jlink) ===
REM En producción (instalado): el JRE está junto a los binarios
if exist "%~dp0..\jre\bin\java.exe" (
    set JAVA_CMD=%~dp0..\jre\bin\java.exe
    echo Usando JRE embebido: %JAVA_CMD%
) else if exist "%~dp0jre\bin\java.exe" (
    set JAVA_CMD=%~dp0jre\bin\java.exe
    echo Usando JRE embebido: %JAVA_CMD%
) else (
    REM Fallback: usar Java del sistema (modo desarrollo)
    where java >nul 2>nul
    if %ERRORLEVEL% NEQ 0 (
        echo Error: No se encuentra Java embebido ni Java del sistema
        exit /b 1
    )
    set JAVA_CMD=java
    echo Usando Java del sistema
)

"%JAVA_CMD%" -Dspring.profiles.active=offline -jar "%JAR_PATH%"
