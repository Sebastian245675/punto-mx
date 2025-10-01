@echo off
echo ⚡ Compilación rápida de Kriol POS
echo.

cd /d "d:\unicenta-pos"

echo 🔄 Compilando solo los cambios...
mvn compile -DskipTests -pl kriolos-opos-app

if %ERRORLEVEL% == 0 (
    echo.
    echo ✅ ¡Compilación exitosa!
    echo 🚀 Para ejecutar: mvn spring-boot:run -pl kriolos-opos-app
    echo.
    echo 💡 Tip: Cada vez que hagas cambios, ejecuta este script
) else (
    echo.
    echo ❌ Error en la compilación
)

pause