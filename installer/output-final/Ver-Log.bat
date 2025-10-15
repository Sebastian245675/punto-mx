@echo off
title SebastianPOS - Ver Log
echo.
echo ========================================
echo      SEBASTIAN POS - Archivo de Log
echo ========================================
echo.
if exist "%~dp0SebastianPOS-log.txt" (
    echo Abriendo log en Bloc de notas...
    start notepad.exe "%~dp0SebastianPOS-log.txt"
) else (
    echo [ERROR] No se encontro el archivo de log.
    echo.
    echo El log se crea cuando ejecutas la aplicacion.
    echo Ubicacion: %~dp0SebastianPOS-log.txt
    echo.
    pause
)
exit
