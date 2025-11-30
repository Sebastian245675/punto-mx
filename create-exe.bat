@echo off
REM Script para crear un ejecutable .exe usando jpackage
REM Requiere JDK 14+ con jpackage incluido

echo ========================================
echo Creando ejecutable .exe para KriolOS POS
echo ========================================
echo.

REM Verificar que Java está disponible
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java no está instalado o no está en el PATH
    pause
    exit /b 1
)

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
    echo.
    echo ========================================
    echo ADVERTENCIA: jpackage no está disponible
    echo ========================================
    echo.
    echo jpackage requiere JDK 14 o superior.
    echo.
    echo Alternativa: Usar Launch4j para crear un .exe wrapper
    echo Descargar desde: https://launch4j.sourceforge.net/
    echo.
    echo O usar el launcher optimizado: kriolos-pos-launcher.bat
    echo.
    pause
    exit /b 1
)

echo Creando ejecutable con jpackage...
echo.

REM Crear directorio de salida si no existe
if not exist "dist" mkdir dist

REM Crear el ejecutable usando jpackage
REM Opciones optimizadas para inicio rápido:
REM --java-options: Opciones JVM para inicio rápido
REM --main-class: Clase principal
REM --name: Nombre de la aplicación
REM --app-version: Versión
REM --input: Directorio con el JAR
REM --main-jar: JAR principal
REM --dest: Directorio de salida
REM --type: Tipo de paquete (app-image para solo ejecutable, exe para instalador)

"%JAVA_HOME%\bin\jpackage.exe" ^
    --type app-image ^
    --name "KriolOS-POS" ^
    --app-version "1.0.0" ^
    --description "KriolOS Point of Sales System" ^
    --vendor "KriolOS" ^
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
    --dest "dist" ^
    --win-console

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo ¡Ejecutable creado exitosamente!
    echo ========================================
    echo.
    echo El ejecutable se encuentra en: dist\KriolOS-POS\KriolOS-POS.exe
    echo.
    echo Puedes copiar la carpeta completa "KriolOS-POS" a donde quieras
    echo y ejecutar KriolOS-POS.exe directamente.
    echo.
) else (
    echo.
    echo ========================================
    echo ERROR: Fallo al crear el ejecutable
    echo ========================================
    echo.
)

pause

