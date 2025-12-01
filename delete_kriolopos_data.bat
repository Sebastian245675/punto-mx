@echo off
REM Script para eliminar carpeta de datos de MySQL y forzar recreación

setlocal enabledelayedexpansion

echo.
echo === ELIMINANDO BASE DE DATOS KRIOLOPOS ===
echo.

REM Intentar detener MySQL primero
echo Deteniendo servicio MySQL...
net stop MySQL80 /y 2>nul
timeout /t 2 /nobreak

REM Buscar y eliminar carpetas de datos
echo Buscando carpetas de datos MySQL...

for /d %%D in ("C:\ProgramData\MySQL\MySQL Server*") do (
    if exist "%%D\Data\kriolopos" (
        echo Eliminando: %%D\Data\kriolopos
        rmdir /s /q "%%D\Data\kriolopos"
        echo ✓ Carpeta eliminada
    )
)

REM Reiniciar MySQL
echo.
echo Reiniciando servicio MySQL...
net start MySQL80
timeout /t 3 /nobreak

echo.
echo ✓ Base de datos eliminada. Se recreará automáticamente en el próximo inicio de la aplicación.
echo.
pause
