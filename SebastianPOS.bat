@echo off
title Sebastian POS - Sistema de Punto de Venta
color 0A

echo =====================================
echo    SEBASTIAN POS - INICIANDO
echo =====================================
echo.

REM Verificar si Java está instalado
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java no está instalado o no está en el PATH
    echo.
    echo Por favor instala Java 11 o superior desde:
    echo https://adoptium.net/
    echo.
    pause
    exit /b 1
)

echo ✓ Java detectado correctamente
echo.

REM Buscar el JAR compilado
cd /d "%~dp0kriolos-opos-app\target"

REM Buscar el JAR principal (no el -original)
for %%f in (kriolos-pos-*.jar) do (
    if not "%%f"=="kriolos-pos.jar.original" (
        set JAR_FILE=%%f
        goto :found_jar
    )
)

:found_jar
if not defined JAR_FILE (
    echo ERROR: No se encontró el JAR compilado
    echo.
    echo Por favor ejecuta primero: mvn clean install -DskipTests
    echo.
    pause
    exit /b 1
)

echo Iniciando Sebastian POS con %JAR_FILE%...
echo.
cd /d "%~dp0kriolos-opos-app\target"
java -jar "%JAR_FILE%"

if errorlevel 1 (
    echo.
    echo ERROR: La aplicación se cerró inesperadamente
    echo.
    pause
)

echo.
echo Gracias por usar Sebastian POS
pause