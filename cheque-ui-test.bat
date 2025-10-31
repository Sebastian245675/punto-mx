@echo off
echo ================================
echo CHEQUE UI Enhancement - Misma metodología
echo ================================
echo.
echo Aplicando los MISMOS cambios que Cash:
echo.
echo 1. Keypad: 20x20px (mini cuadradito)
echo 2. Campo entrada: tamaño original + fuente +2pt
echo 3. Campo "Given": 300x45px + fuente +4pt
echo 4. Panel contenedor expandido: 400x60px
echo 5. Posiciones corregidas para verse completo
echo.
echo Compilando...
call mvn compile -q
if %ERRORLEVEL% neq 0 (
    echo ERROR en compilacion
    pause
    exit /b 1
)
echo.
echo ✓ Compilacion OK!
echo.
echo ================================
echo PARA PROBAR:
echo ================================
echo 1. Ejecuta la aplicacion
echo 2. Ve al panel de pagos "Cheque"
echo 3. Deberia verse igual que Cash:
echo    - Keypad mini (casi invisible)
echo    - Campo "Given" grande y claro
echo    - Todo funcional
echo.
echo ¡La metodología funciona para ambos!
echo ================================
pause