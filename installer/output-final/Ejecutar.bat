@echo off
title SebastianPOS - Ejecutar
echo.
echo ========================================
echo        SEBASTIAN POS - Iniciando
echo ========================================
echo.
start "" "%~dp0SebastianPOS.exe"
echo Aplicacion iniciada.
echo Log: %~dp0SebastianPOS-log.txt
echo.
timeout /t 3
exit
