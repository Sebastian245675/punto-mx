@echo off
echo ========================================
echo Generando EXE optimizado
echo ========================================
echo.

REM Verificar JAR
if not exist "kriolos-opos-app\target\kriolos-pos.jar" (
    echo ERROR: JAR no encontrado. Compilando...
    call mvn clean package -DskipTests
    if errorlevel 1 (
        echo ERROR: Fallo la compilacion
        pause
        exit /b 1
    )
)

echo JAR encontrado: kriolos-opos-app\target\kriolos-pos.jar

REM Buscar Launch4j
set LAUNCH4J=
if exist "C:\Program Files\Launch4j\launch4j.exe" set LAUNCH4J=C:\Program Files\Launch4j\launch4j.exe
if exist "C:\Program Files (x86)\Launch4j\launch4j.exe" set LAUNCH4J=C:\Program Files (x86)\Launch4j\launch4j.exe
if exist "launch4j\launch4j.exe" set LAUNCH4J=%CD%\launch4j\launch4j.exe

if "%LAUNCH4J%"=="" (
    echo.
    echo Launch4j no encontrado.
    echo Por favor, descarga Launch4j desde: https://launch4j.sourceforge.net/
    echo O instala Launch4j y vuelve a ejecutar este script.
    echo.
    pause
    exit /b 1
)

echo Usando Launch4j: %LAUNCH4J%
echo.

REM Crear directorio en Descargas del disco D
set DIST_DIR=D:\Descargas
if not exist "%DIST_DIR%" (
    set DIST_DIR=D:\Downloads
    if not exist "%DIST_DIR%" (
        mkdir D:\Descargas
        set DIST_DIR=D:\Descargas
    )
)
echo Directorio de salida: %DIST_DIR%

REM Actualizar configuracion para usar disco D
powershell -Command "(Get-Content 'launch4j-config.xml') -replace '<outfile>dist\\\\CONNECTING-POS.exe</outfile>', '<outfile>%DIST_DIR%\\CONNECTING-POS.exe</outfile>' | Set-Content 'launch4j-config-temp.xml'"

REM Generar EXE
echo Generando EXE...
"%LAUNCH4J%" launch4j-config-temp.xml

if exist "%DIST_DIR%\CONNECTING-POS.exe" (
    echo.
    echo ========================================
    echo EXE generado exitosamente!
    echo ========================================
    echo.
    for %%F in ("%DIST_DIR%\CONNECTING-POS.exe") do (
        set /a SIZE_MB=%%~zF/1048576
        echo Tamaño: %%~zF bytes (~%SIZE_MB% MB^)
    )
    echo.
    echo Ubicación: %DIST_DIR%\CONNECTING-POS.exe
    del launch4j-config-temp.xml 2>nul
    echo.
) else (
    echo ERROR: No se pudo generar el EXE
    echo Verifica que launch4j-config.xml existe y es válido
    echo.
)

pause

