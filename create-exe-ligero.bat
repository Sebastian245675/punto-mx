@echo off
REM Script para crear un ejecutable .exe LIGERO (sin JRE)
REM Requiere Java instalado en el sistema destino
REM Tamaño aproximado: ~50-80 MB (solo JAR + dependencias)

echo ========================================
echo Creando ejecutable LIGERO .exe
echo ========================================
echo.

REM Verificar que el JAR existe
set APP_JAR=kriolos-opos-app\target\kriolos-pos.jar
if not exist "%APP_JAR%" (
    echo ERROR: No se encuentra el archivo JAR: %APP_JAR%
    echo Por favor, compile el proyecto primero con: mvn clean install -DskipTests
    pause
    exit /b 1
)

REM Obtener la ruta de Java
for /f "tokens=*" %%i in ('where java') do set JAVA_PATH=%%i
set JAVA_HOME=%JAVA_PATH:~0,-11%

echo Java encontrado en: %JAVA_HOME%
echo JAR encontrado: %APP_JAR%
echo.

REM Verificar si jpackage está disponible
"%JAVA_HOME%\bin\jpackage.exe" --version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: jpackage no está disponible
    echo Requiere JDK 14 o superior
    pause
    exit /b 1
)

echo Creando ejecutable ligero (sin JRE incluido)...
echo NOTA: El ejecutable requerirá Java instalado en el sistema destino
echo.

REM Crear directorio de salida si no existe
if not exist "dist-ligero" mkdir dist-ligero

REM Crear el ejecutable SIN incluir el runtime JRE
REM Esto hace que el ejecutable sea mucho más pequeño (~50-80 MB vs ~360 MB)
"%JAVA_HOME%\bin\jpackage.exe" ^
    --type app-image ^
    --name "CONNECTING-POS-Lite" ^
    --app-version "1.0.0" ^
    --description "CONNECTING POS - Sistema de Punto de Venta (Ligero)" ^
    --vendor "CONNECTING POS" ^
    --input "kriolos-opos-app\target" ^
    --main-jar "kriolos-pos.jar" ^
    --main-class "com.openbravo.pos.forms.StartPOS" ^
    --java-options "-Xms256m" ^
    --java-options "-Xmx2g" ^
    --java-options "-XX:+UseG1GC" ^
    --java-options "-XX:+UseStringDeduplication" ^
    --java-options "-Dsun.java2d.d3d=false" ^
    --java-options "-Dsun.java2d.noddraw=true" ^
    --java-options "-Xverify:none" ^
    --java-options "-XX:TieredStopAtLevel=1" ^
    --java-options "-XX:+TieredCompilation" ^
    --java-options "-Dfile.encoding=UTF-8" ^
    --dest "dist-ligero" ^
    --win-console

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo ¡Ejecutable LIGERO creado exitosamente!
    echo ========================================
    echo.
    echo El ejecutable se encuentra en: dist-ligero\CONNECTING-POS-Lite\CONNECTING-POS-Lite.exe
    echo.
    echo IMPORTANTE: Este ejecutable requiere Java 21+ instalado en el sistema destino
    echo Tamaño aproximado: ~50-80 MB (vs ~360 MB con JRE incluido)
    echo.
) else (
    echo.
    echo ========================================
    echo ERROR: Fallo al crear el ejecutable
    echo ========================================
    echo.
)

pause

