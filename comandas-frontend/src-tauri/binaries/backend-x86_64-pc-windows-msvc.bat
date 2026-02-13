@echo off
setlocal

REM Buscar el JAR en modo desarrollo o producción
if exist "%~dp0backend.jar" (
    set JAR_PATH=%~dp0backend.jar
) else if exist "%~dp0..\binaries\backend.jar" (
    set JAR_PATH=%~dp0..\binaries\backend.jar
) else (
    echo Error: No se encuentra backend.jar
    exit /b 1
)

java -Dspring.profiles.active=offline -jar "%JAR_PATH%"
