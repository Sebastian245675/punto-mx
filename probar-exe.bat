@echo off
echo ========================================
echo Ejecutando CONNECTING POS
echo ========================================
echo.

if not exist "dist\CONNECTING-POS\CONNECTING-POS.exe" (
    echo ERROR: No se encuentra el ejecutable
    echo Por favor, ejecuta primero: create-exe.bat
    pause
    exit /b 1
)

echo Iniciando CONNECTING POS...
echo.

cd dist\CONNECTING-POS
start "" "CONNECTING-POS.exe"

echo.
echo La aplicacion se esta iniciando...
echo.
pause

