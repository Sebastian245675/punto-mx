@echo off
REM Script para crear ejecutable .exe optimizado (<= 150 MB)
setlocal enabledelayedexpansion

echo ========================================
echo Generador de EXE Optimizado (^<= 150 MB)
echo ========================================
echo.

REM Verificar PowerShell
powershell -Command "exit 0" >nul 2>&1
if errorlevel 1 (
    echo [ERROR] PowerShell no esta disponible
    echo Por favor, ejecuta este script desde PowerShell o instala PowerShell
    pause
    exit /b 1
)

REM Ejecutar script PowerShell
powershell -ExecutionPolicy Bypass -File "%~dp0crear-exe-150mb.ps1"

if errorlevel 1 (
    echo.
    echo [ERROR] Error al generar el ejecutable
    pause
    exit /b 1
)

echo.
echo Presiona cualquier tecla para cerrar...
pause >nul




