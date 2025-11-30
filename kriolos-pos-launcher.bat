@echo off
REM Launcher optimizado para KriolOS POS - Inicio rápido
REM Este script está optimizado para iniciar la aplicación en ~5 segundos

REM Ocultar la ventana de consola (opcional, descomentar si se quiere)
REM if not "%1"=="show" (
REM     start /min "" "%~f0" show
REM     exit
REM )

REM Configurar variables
set APP_JAR=kriolos-opos-app\target\kriolos-pos.jar
set MAIN_CLASS=com.openbravo.pos.forms.StartPOS

REM Verificar que el JAR existe
if not exist "%APP_JAR%" (
    echo ERROR: No se encuentra el archivo JAR: %APP_JAR%
    echo Por favor, compile el proyecto primero con: mvn clean install -DskipTests
    pause
    exit /b 1
)

REM Configurar opciones JVM para inicio rápido
REM -Xms y -Xmx: Memoria inicial y máxima (ajustar según necesidad)
REM -XX:+UseG1GC: Usar G1 Garbage Collector (mejor para inicio rápido)
REM -XX:+UseStringDeduplication: Optimizar strings
REM -Dsun.java2d.d3d=false: Desactivar aceleración 3D (puede causar problemas)
REM -Dsun.java2d.noddraw=true: Desactivar DirectDraw
REM -Djava.awt.headless=false: Asegurar modo gráfico
REM -Xverify:none: Desactivar verificación de bytecode (más rápido pero menos seguro)
REM -XX:TieredStopAtLevel=1: Compilación JIT más rápida (menos optimización)
REM -XX:+TieredCompilation: Compilación por niveles
REM -Dfile.encoding=UTF-8: Encoding UTF-8

set JVM_OPTS=-Xms256m -Xmx2g ^
    -XX:+UseG1GC ^
    -XX:+UseStringDeduplication ^
    -Dsun.java2d.d3d=false ^
    -Dsun.java2d.noddraw=true ^
    -Djava.awt.headless=false ^
    -Xverify:none ^
    -XX:TieredStopAtLevel=1 ^
    -XX:+TieredCompilation ^
    -Dfile.encoding=UTF-8 ^
    -Dsplash=true

REM Ejecutar la aplicación
echo Iniciando KriolOS POS...
start "" java %JVM_OPTS% -jar "%APP_JAR%"

REM Salir sin pausa para inicio más rápido
exit /b 0

