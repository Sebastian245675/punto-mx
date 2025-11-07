@echo off
echo ========================================
echo Compilando proyecto (saltando tests)...
echo ========================================
call mvn clean install -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: La compilacion fallo
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ========================================
echo Ejecutando aplicacion...
echo ========================================
java -jar kriolos-opos-app\target\kriolos-pos.jar

pause

