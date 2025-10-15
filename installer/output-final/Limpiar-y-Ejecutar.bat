@echo off
title SebastianPOS - Limpiar y Ejecutar
color 0E
echo.
echo ========================================
echo   SEBASTIAN POS - Regenerar Base Datos
echo ========================================
echo.
echo [ADVERTENCIA] Esto eliminara la base de datos actual
echo y creara una nueva con datos predeterminados.
echo.
pause
echo.
echo [1/3] Cerrando procesos Java...
taskkill /F /IM java.exe 2>nul
taskkill /F /IM javaw.exe 2>nul
timeout /t 2 /nobreak >nul

echo [2/3] Eliminando base de datos...
if exist "%USERPROFILE%\kriolopos" (
    rd /s /q "%USERPROFILE%\kriolopos"
    echo      Base de datos eliminada
) else (
    echo      No habia base de datos previa
)

echo [3/3] Iniciando aplicacion...
echo      (Primera vez tarda 30-60 segundos)
echo.
start "" "%~dp0SebastianPOS.exe"
echo.
echo ========================================
echo   Aplicacion iniciada correctamente
echo ========================================
echo.
echo El archivo de log se guarda en:
echo %~dp0SebastianPOS-log.txt
echo.
timeout /t 5
exit
