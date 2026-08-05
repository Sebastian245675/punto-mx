@echo off
REM Esperar a que Maven termine y luego generar EXE

setlocal enabledelayedexpansion
cd /d D:\punto-mx

echo Esperando que Maven termine la compilacion...
echo.

:wait_loop
if exist "kriolos-opos-app\target\kriolos-pos.jar" (
    echo JAR listo. Continuando con Launch4j...
    goto :generate_exe
) else (
    echo Esperando... %date% %time%
    timeout /t 10 /nobreak
    goto :wait_loop
)

:generate_exe
echo.
echo ============================================
echo Buscando Launch4j...
echo ============================================

set "LAUNCH4J="
if exist "C:\Program Files\Launch4j\launch4jc.exe" (
    set "LAUNCH4J=C:\Program Files\Launch4j\launch4jc.exe"
) else if exist "C:\Program Files (x86)\Launch4j\launch4jc.exe" (
    set "LAUNCH4J=C:\Program Files (x86)\Launch4j\launch4jc.exe"
)

if "!LAUNCH4J!"=="" (
    echo.
    echo [ERROR] Launch4j no encontrado!
    echo.
    echo Por favor descarga e instala Launch4j desde:
    echo http://launch4j.sourceforge.net/
    echo.
    echo Directorio recomendado: C:\Program Files\Launch4j
    echo.
    pause
    exit /b 1
)

echo Encontrado: !LAUNCH4J!
echo.

set "JAR_PATH=%cd%\kriolos-opos-app\target\kriolos-pos.jar"
set "CONFIG_PATH=%cd%\launch4j-150mb.xml"
set "OUTPUT_EXE=D:\Descargas\CONNECTING-POS.exe"

echo Generando configuracion de Launch4j...
(
echo ^<?xml version="1.0" encoding="UTF-8"?^>
echo ^<launch4jConfig^>
echo   ^<dontWrapJar^>false^</dontWrapJar^>
echo   ^<headerType^>gui^</headerType^>
echo   ^<jar^>%JAR_PATH%^</jar^>
echo   ^<outfile^>%OUTPUT_EXE%^</outfile^>
echo   ^<errTitle^>CONNECTING POS Error^</errTitle^>
echo   ^<cmdLine^>^</cmdLine^>
echo   ^<chdir^>.^</chdir^>
echo   ^<priority^>normal^</priority^>
echo   ^<downloadUrl^>https://adoptium.net/^</downloadUrl^>
echo   ^<supportUrl^>https://github.com/Sebastian245675/punto-mx^</supportUrl^>
echo   ^<stayAlive^>false^</stayAlive^>
echo   ^<restartOnCrash^>false^</restartOnCrash^>
echo   ^<manifest^>^</manifest^>
echo   ^<icon^>^</icon^>
echo   ^<jre^>
echo     ^<path^>%%JAVA_HOME%%\bin^</path^>
echo     ^<requiresJdk^>false^</requiresJdk^>
echo     ^<requires64Bit^>false^</requires64Bit^>
echo     ^<minVersion^>21^</minVersion^>
echo     ^<maxVersion^>^</maxVersion^>
echo     ^<opt^>-Xmx1024m^</opt^>
echo     ^<opt^>-Djava.util.logging.config.file=logging.properties^</opt^>
echo   ^</jre^>
echo   ^<versionInfo^>
echo     ^<fileVersion^>1.0.0.0^</fileVersion^>
echo     ^<txtFileVersion^>1.0.0^</txtFileVersion^>
echo     ^<fileDescription^>CONNECTING POS - Sistema de Punto de Venta^</fileDescription^>
echo     ^<copyright^>2025 CONNECTING POS^</copyright^>
echo     ^<productVersion^>1.0.0.0^</productVersion^>
echo     ^<txtProductVersion^>1.0.0^</txtProductVersion^>
echo     ^<productName^>CONNECTING POS^</productName^>
echo     ^<companyName^>CONNECTING POS^</companyName^>
echo     ^<internalName^>CONNECTING-POS^</internalName^>
echo     ^<originalFilename^>CONNECTING-POS.exe^</originalFilename^>
echo   ^</versionInfo^>
echo ^</launch4jConfig^>
) > "%CONFIG_PATH%"

echo Ejecutando Launch4j...
echo.
"!LAUNCH4J!" "%CONFIG_PATH%"

if errorlevel 1 (
    echo [ERROR] Error al generar EXE
    pause
    exit /b 1
)

echo.
if exist "%OUTPUT_EXE%" (
    for /F "usebackq" %%A in ('%OUTPUT_EXE%') do set "EXE_SIZE=%%~zA"
    echo.
    echo ============================================
    echo EXE GENERADO EXITOSAMENTE
    echo ============================================
    echo Ubicacion: %OUTPUT_EXE%
    echo Tamanio: %EXE_SIZE% bytes
    for /F %%A in ('powershell -Command "[Math]::Round(%EXE_SIZE%/1048576, 2)"') do set "EXE_SIZE_MB=%%A"
    echo Tamanio: %EXE_SIZE_MB% MB
    echo ============================================
) else (
    echo ERROR: No se pudo generar el EXE
    pause
    exit /b 1
)

pause
