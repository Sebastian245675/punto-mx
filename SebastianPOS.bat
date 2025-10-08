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

REM Verificar si Maven está disponible
where mvn >nul 2>&1
if errorlevel 1 (
    echo ADVERTENCIA: Maven no encontrado, usando método alternativo...
    echo.
    
    REM Método alternativo: ejecutar directamente con Java
    cd /d "%~dp0kriolos-opos-app"
    
    if not exist "target\classes" (
        echo ERROR: La aplicación no está compilada
        echo Ejecuta primero: mvn clean install
        pause
        exit /b 1
    )
    
    echo Iniciando Sebastian POS...
    echo.
    java -cp "target\classes;target\dependency\*" com.openbravo.pos.forms.StartPOS
) else (
    echo ✓ Maven detectado correctamente
    echo.
    echo Iniciando Sebastian POS...
    echo.
    cd /d "%~dp0kriolos-opos-app"
    mvn exec:java -q
)

if errorlevel 1 (
    echo.
    echo ERROR: La aplicación se cerró inesperadamente
    echo.
    pause
)

echo.
echo Gracias por usar Sebastian POS
pause