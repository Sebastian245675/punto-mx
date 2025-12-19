@echo off
setlocal enabledelayedexpansion
echo ========================================
echo Generando EXE optimizado (menos de 120 MB)
echo ========================================
echo.

REM Verificar que el JAR existe
if not exist "kriolos-opos-app\target\kriolos-pos.jar" (
    echo ERROR: No se encuentra el JAR compilado
    echo Por favor, ejecuta primero: mvn clean package -DskipTests
    pause
    exit /b 1
)

echo Verificando tamaño del JAR...
for %%F in ("kriolos-opos-app\target\kriolos-pos.jar") do (
    set /a JAR_SIZE_MB=%%~zF/1048576
    echo Tamaño del JAR: %%~zF bytes (~!JAR_SIZE_MB! MB^)
)

REM Crear directorio de salida
if not exist "dist" mkdir dist

REM Buscar Launch4j
set LAUNCH4J_PATH=
if exist "C:\Program Files\Launch4j\launch4j.exe" (
    set LAUNCH4J_PATH=C:\Program Files\Launch4j\launch4j.exe
    goto :found_launch4j
)
if exist "C:\Program Files (x86)\Launch4j\launch4j.exe" (
    set LAUNCH4J_PATH=C:\Program Files (x86)\Launch4j\launch4j.exe
    goto :found_launch4j
)
if exist "launch4j\launch4j.exe" (
    set LAUNCH4J_PATH=%CD%\launch4j\launch4j.exe
    goto :found_launch4j
)

REM Launch4j no encontrado - intentar descargarlo
echo.
echo Launch4j no encontrado. Intentando descargarlo...
if not exist "launch4j" mkdir launch4j

REM Verificar si wget o curl están disponibles
set DOWNLOAD_CMD=
where wget >nul 2>&1 && set DOWNLOAD_CMD=wget -O
where curl >nul 2>&1 && set DOWNLOAD_CMD=curl -L -o

if "!DOWNLOAD_CMD!"=="" (
    echo.
    echo ERROR: No se encontró Launch4j y no hay herramienta de descarga disponible.
    echo.
    echo Por favor, descarga Launch4j manualmente desde:
    echo https://sourceforge.net/projects/launch4j/files/launch4j-3/3.50/launch4j-3.50-windows-x64.exe/download
    echo.
    echo O instala Launch4j desde: https://launch4j.sourceforge.net/
    echo.
    set /p LAUNCH4J_PATH="Ingresa la ruta completa a launch4j.exe (o presiona Enter para cancelar): "
    if "!LAUNCH4J_PATH!"=="" (
        echo Operación cancelada.
        pause
        exit /b 1
    )
    goto :found_launch4j
)

echo Descargando Launch4j...
%DOWNLOAD_CMD% "launch4j\launch4j.zip" "https://sourceforge.net/projects/launch4j/files/launch4j-3/3.50/launch4j-3.50-windows-x64.zip/download"

if not exist "launch4j\launch4j.zip" (
    echo ERROR: No se pudo descargar Launch4j
    echo Por favor, descárgalo manualmente desde: https://launch4j.sourceforge.net/
    pause
    exit /b 1
)

echo Extrayendo Launch4j...
powershell -Command "Expand-Archive -Path 'launch4j\launch4j.zip' -DestinationPath 'launch4j' -Force"

REM Buscar el ejecutable en la carpeta extraída
for /r "launch4j" %%F in (launch4j.exe) do (
    set LAUNCH4J_PATH=%%F
    goto :found_launch4j
)

:found_launch4j
if not exist "!LAUNCH4J_PATH!" (
    echo ERROR: No se puede encontrar Launch4j en: !LAUNCH4J_PATH!
    pause
    exit /b 1
)

echo.
echo Usando Launch4j: !LAUNCH4J_PATH!
echo.

REM Crear configuración temporal de Launch4j optimizada
set TEMP_CONFIG=%TEMP%\launch4j-config-temp.xml
(
echo ^<?xml version="1.0" encoding="UTF-8"?^>
echo ^<launch4jConfig^>
echo   ^<dontWrapJar^>false^</dontWrapJar^>
echo   ^<headerType^>gui^</headerType^>
echo   ^<jar^>%CD%\kriolos-opos-app\target\kriolos-pos.jar^</jar^>
echo   ^<outfile^>%CD%\dist\CONNECTING-POS.exe^</outfile^>
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
echo     ^<path^>^</path^>
echo     ^<bundledJre64Bit^>false^</bundledJre64Bit^>
echo     ^<bundledJreAsFallback^>false^</bundledJreAsFallback^>
echo     ^<minVersion^>21^</minVersion^>
echo     ^<maxVersion^>^</maxVersion^>
echo     ^<jdkPreference^>preferJre^</jdkPreference^>
echo     ^<runtimeBits^>64/32^</runtimeBits^>
echo     ^<opt^>-Djava.io.tmpdir=%%TEMP%%^</opt^>
echo     ^<opt^>-Xms512m^</opt^>
echo     ^<opt^>-Xmx2048m^</opt^>
echo     ^<opt^>-XX:+UseG1GC^</opt^>
echo     ^<opt^>-XX:MaxGCPauseMillis=200^</opt^>
echo     ^<opt^>-Dsun.java2d.d3d=false^</opt^>
echo     ^<opt^>-Dsun.java2d.opengl=false^</opt^>
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
) > "%TEMP_CONFIG%"

echo Generando EXE...
"!LAUNCH4J_PATH!" "%TEMP_CONFIG%"

if exist "dist\CONNECTING-POS.exe" (
    echo.
    echo ========================================
    echo EXE generado exitosamente!
    echo ========================================
    echo.
    for %%F in ("dist\CONNECTING-POS.exe") do (
        set /a EXE_SIZE_MB=%%~zF/1048576
        echo Tamaño del EXE: %%~zF bytes (~!EXE_SIZE_MB! MB^)
        if !EXE_SIZE_MB! LSS 120 (
            echo [OK] El EXE es menor a 120 MB - Objetivo cumplido!
        ) else (
            echo [ADVERTENCIA] El EXE es mayor a 120 MB
        )
    )
    echo.
    echo Ubicación: %CD%\dist\CONNECTING-POS.exe
    echo.
    echo NOTA: Este EXE requiere Java 21 o superior instalado en el sistema.
    echo Si Java no está instalado, el usuario será dirigido a descargarlo.
    echo.
) else (
    echo.
    echo ERROR: No se pudo generar el EXE
    echo Verifica los logs de Launch4j para más detalles
    echo.
)

REM Limpiar archivo temporal
if exist "%TEMP_CONFIG%" del "%TEMP_CONFIG%"

pause





