@echo off
REM Script para crear un ejecutable MINIMO (~100 MB máximo)
REM Solo incluye el JAR y dependencias esenciales

echo ========================================
echo Creando ejecutable MINIMO
echo ========================================
echo.

set APP_JAR=kriolos-opos-app\target\kriolos-pos.jar
if not exist "%APP_JAR%" (
    echo ERROR: No se encuentra el JAR
    pause
    exit /b 1
)

REM Crear directorio de salida
if not exist "dist-minimo" mkdir dist-minimo
if not exist "dist-minimo\CONNECTING-POS" mkdir dist-minimo\CONNECTING-POS

echo Copiando archivos esenciales...
copy "%APP_JAR%" "dist-minimo\CONNECTING-POS\" >nul

REM Copiar solo las librerías más críticas (reducir tamaño)
REM En lugar de copiar todas, creamos un launcher que use el classpath del sistema
echo Creando launcher...

REM Crear un launcher .bat optimizado
(
echo @echo off
echo REM Launcher para CONNECTING POS
echo setlocal
echo.
echo REM Buscar Java
echo where java ^>nul 2^>^&1
echo if %%ERRORLEVEL%% NEQ 0 ^(
echo     echo ERROR: Java no encontrado. Por favor instala Java 21 o superior.
echo     echo Descarga desde: https://adoptium.net/
echo     pause
echo     exit /b 1
echo ^)
echo.
echo REM Ejecutar aplicación
echo java -Xms256m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true -Xverify:none -XX:TieredStopAtLevel=1 -XX:+TieredCompilation -Dfile.encoding=UTF-8 -jar "%%~dp0kriolos-pos.jar"
echo.
) > "dist-minimo\CONNECTING-POS\CONNECTING-POS.bat"

echo.
echo ========================================
echo Ejecutable MINIMO creado
echo ========================================
echo.
echo Ubicación: dist-minimo\CONNECTING-POS\
echo.
echo Para crear un .exe desde el .bat, puedes usar:
echo - Bat To Exe Converter: https://www.battoexeconverter.com/
echo - O simplemente usar el .bat directamente
echo.
echo Tamaño aproximado: Solo el JAR (~120 MB)
echo.
pause

