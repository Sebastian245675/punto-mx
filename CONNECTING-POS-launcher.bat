@echo off
REM CONNECTING-POS - Launcher
REM Este script lanza la aplicación Java

setlocal enabledelayedexpansion

REM Obtener la ruta del script
set "SCRIPT_DIR=%~dp0"

REM Ruta del JAR
set "JAR_PATH=%SCRIPT_DIR%kriolos-pos.jar"

REM Verificar Java
java -version >nul 2>&1
if errorlevel 1 (
    echo Error: Java no encontrado en el PATH
    echo Por favor instala Java 21 o superior
    pause
    exit /b 1
)

REM Ejecutar la aplicación
java -Xmx1024m -Dsun.java2d.uiScale=1.0 -jar "%JAR_PATH%" %*

exit /b %errorlevel%
