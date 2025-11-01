@echo off
echo ================================
echo UI Simple Enhancement
echo ================================
echo.
echo Cambios MINIMOS aplicados:
echo.
echo 1. Keypad: 20x20px (mini cuadradito en esquina)
echo 2. Campo entrada: 130x30px (tamaño original + fuente)
echo 3. Campos Given/Change: reposicionados para verse completos
echo 4. Fuentes ligeramente más grandes
echo.
echo TODO LO DEMAS IGUAL - sin complicaciones!
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
echo Ahora prueba la aplicacion - deberia verse mejor
echo pero funcionar exactamente igual.
echo.
pause