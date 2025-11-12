@echo off
echo ========================================
echo Ejecutando Kriolos POS con logs...
echo ========================================
echo.
java -jar kriolos-opos-app\target\kriolos-pos.jar 2>&1 | findstr /I "DEBUG ADVERTENCIA ERROR permisos rol usuario Administrator"
pause
