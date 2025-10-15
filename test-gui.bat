@echo off
echo ========================================
echo Sebastian POS - Test de GUI
echo ========================================
echo.

cd /d "%~dp0kriolos-opos-app\target"

echo Ejecutando JAR con logging...
echo.

java -Djava.util.logging.config.file=logging.properties -jar kriolos-pos.jar

echo.
echo Proceso terminado
pause
