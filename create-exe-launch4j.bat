@echo off
REM Script para crear un ejecutable .exe usando Launch4j
REM Requiere Launch4j instalado: https://launch4j.sourceforge.net/

echo ========================================
echo Creando ejecutable .exe con Launch4j
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

REM Verificar si Launch4j está disponible
set LAUNCH4J_PATH=
if exist "C:\Program Files\Launch4j\launch4j.exe" (
    set LAUNCH4J_PATH=C:\Program Files\Launch4j\launch4j.exe
) else if exist "C:\Program Files (x86)\Launch4j\launch4j.exe" (
    set LAUNCH4J_PATH=C:\Program Files (x86)\Launch4j\launch4j.exe
) else (
    echo.
    echo ========================================
    echo ADVERTENCIA: Launch4j no está instalado
    echo ========================================
    echo.
    echo Por favor, descarga e instala Launch4j desde:
    echo https://launch4j.sourceforge.net/
    echo.
    echo O usa el launcher optimizado: kriolos-pos-launcher.bat
    echo.
    pause
    exit /b 1
)

echo Launch4j encontrado en: %LAUNCH4J_PATH%
echo JAR encontrado: %APP_JAR%
echo.

REM Crear directorio de salida si no existe
if not exist "dist" mkdir dist

REM Crear archivo de configuración XML para Launch4j
set CONFIG_XML=dist\launch4j-config.xml

echo Creando archivo de configuración de Launch4j...
(
echo ^<?xml version="1.0" encoding="UTF-8"?^>
echo ^<launch4jConfig^>
echo   ^<dontWrapJar^>false^</dontWrapJar^>
echo   ^<headerType^>gui^</headerType^>
echo   ^<jar^>%CD%\%APP_JAR%^</jar^>
echo   ^<outfile^>%CD%\dist\KriolOS-POS.exe^</outfile^>
echo   ^<errTitle^>KriolOS POS Error^</errTitle^>
echo   ^<cmdLine^>^</cmdLine^>
echo   ^<chdir^>%CD%^</chdir^>
echo   ^<priority^>normal^</priority^>
echo   ^<downloadUrl^>http://java.com/download^</downloadUrl^>
echo   ^<supportUrl^>^</supportUrl^>
echo   ^<stayAlive^>false^</stayAlive^>
echo   ^<restartOnCrash^>false^</restartOnCrash^>
echo   ^<manifest^>^</manifest^>
echo   ^<icon^>^</icon^>
echo   ^<jre^>
echo     ^<path^>^</path^>
echo     ^<bundledJre64Bit^>false^</bundledJre64Bit^>
echo     ^<bundledJreAsFallback^>false^</bundledJreAsFallback^>
echo     ^<minVersion^>21^</minVersion^>
echo     ^<maxVersion^>^</maxVersion^>
echo     ^<jdkPreference^>preferJre^</jdkPreference^>
echo     ^<runtimeBits^>64/32^</runtimeBits^>
echo     ^<initialHeapSize^>256^</initialHeapSize^>
echo     ^<maxHeapSize^>2048^</maxHeapSize^>
echo     ^<opt^>-XX:+UseG1GC^</opt^>
echo     ^<opt^>-XX:+UseStringDeduplication^</opt^>
echo     ^<opt^>-Dsun.java2d.d3d=false^</opt^>
echo     ^<opt^>-Dsun.java2d.noddraw=true^</opt^>
echo     ^<opt^>-Xverify:none^</opt^>
echo     ^<opt^>-XX:TieredStopAtLevel=1^</opt^>
echo     ^<opt^>-XX:+TieredCompilation^</opt^>
echo     ^<opt^>-Dfile.encoding=UTF-8^</opt^>
echo   ^</jre^>
echo ^</launch4jConfig^>
) > "%CONFIG_XML%"

echo Archivo de configuración creado: %CONFIG_XML%
echo.

echo Creando ejecutable con Launch4j...
"%LAUNCH4J_PATH%" "%CONFIG_XML%"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo ¡Ejecutable creado exitosamente!
    echo ========================================
    echo.
    echo El ejecutable se encuentra en: dist\KriolOS-POS.exe
    echo.
    echo Puedes copiar KriolOS-POS.exe a donde quieras y ejecutarlo directamente.
    echo.
) else (
    echo.
    echo ========================================
    echo ERROR: Fallo al crear el ejecutable
    echo ========================================
    echo.
)

pause

