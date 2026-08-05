@echo off
chcp 65001 >nul
echo ========================================
echo Generando EXE con icono (máx 130 MB)
echo ========================================
echo.

REM Verificar JAR
if not exist "kriolos-opos-app\target\kriolos-pos.jar" (
    echo ERROR: JAR no encontrado. Compilando...
    call mvn clean package -DskipTests
    if errorlevel 1 (
        echo ERROR: Falló la compilación
        pause
        exit /b 1
    )
)

REM Verificar tamaño del JAR
for %%F in ("kriolos-opos-app\target\kriolos-pos.jar") do (
    set /a JAR_SIZE_MB=%%~zF/1048576
    echo JAR encontrado: %%~zF bytes (~%JAR_SIZE_MB% MB)
    
    if %JAR_SIZE_MB% GTR 120 (
        echo ADVERTENCIA: El JAR es muy grande (^>120 MB^). El EXE podría exceder 130 MB.
    )
)

echo.

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

REM Buscar archivo de icono
set ICON_FILE=
if exist "icono.ico" set ICON_FILE=%CD%\icono.ico
if exist "icon.ico" set ICON_FILE=%CD%\icon.ico
if exist "resources\icono.ico" set ICON_FILE=%CD%\resources\icono.ico
if exist "kriolos-opos-app\src\main\resources\icono.ico" set ICON_FILE=%CD%\kriolos-opos-app\src\main\resources\icono.ico

if "%ICON_FILE%"=="" (
    echo ADVERTENCIA: No se encontró archivo de icono (.ico)
    echo Creando icono por defecto...
    echo.
    REM Crear un icono simple si no existe (usando PowerShell)
    powershell -Command "$ErrorActionPreference='Stop'; $icon = [System.Drawing.Icon]::ExtractAssociatedIcon([System.Diagnostics.Process]::GetCurrentProcess().MainModule.FileName); $icon.ToBitmap().Save('%CD%\icono.ico', [System.Drawing.Imaging.ImageFormat]::Icon)" 2>nul
    if exist "icono.ico" (
        set ICON_FILE=%CD%\icono.ico
        echo Icono creado: %ICON_FILE%
    ) else (
        echo No se pudo crear icono. Continuando sin icono...
        set ICON_FILE=
    )
) else (
    echo Icono encontrado: %ICON_FILE%
)

echo.

REM Crear directorio de salida
set DIST_DIR=D:\Descargas
if not exist "%DIST_DIR%" (
    set DIST_DIR=D:\Downloads
    if not exist "%DIST_DIR%" (
        mkdir "%DIST_DIR%" 2>nul
        if not exist "%DIST_DIR%" (
            set DIST_DIR=%CD%\dist
            mkdir "%DIST_DIR%" 2>nul
        )
    )
)
echo Directorio de salida: %DIST_DIR%
echo.

REM Obtener ruta absoluta del JAR
for %%F in ("kriolos-opos-app\target\kriolos-pos.jar") do set JAR_PATH=%%~fF

REM Crear configuración temporal de Launch4j
set CONFIG_TEMP=%TEMP%\launch4j-config-temp.xml
(
echo ^<?xml version="1.0" encoding="UTF-8"?^>
echo ^<launch4jConfig^>
echo   ^<dontWrapJar^>false^</dontWrapJar^>
echo   ^<headerType^>gui^</headerType^>
echo   ^<jar^>%JAR_PATH%^</jar^>
echo   ^<outfile^>%DIST_DIR%\CONNECTING-POS.exe^</outfile^>
echo   ^<errTitle^>CONNECTING POS Error^</errTitle^>
echo   ^<cmdLine^>^</cmdLine^>
echo   ^<chdir^>.^</chdir^>
echo   ^<priority^>normal^</priority^>
echo   ^<downloadUrl^>https://adoptium.net/^</downloadUrl^>
echo   ^<supportUrl^>https://github.com/Sebastian245675/punto-mx^</supportUrl^>
echo   ^<stayAlive^>false^</stayAlive^>
echo   ^<restartOnCrash^>false^</restartOnCrash^>
echo   ^<manifest^>^</manifest^>
) > "%CONFIG_TEMP%"

REM Agregar icono si existe
if not "%ICON_FILE%"=="" (
    echo   ^<icon^>%ICON_FILE%^</icon^> >> "%CONFIG_TEMP%"
) else (
    echo   ^<icon^>^</icon^> >> "%CONFIG_TEMP%"
)

REM Agregar configuración JRE y versión
(
echo   ^<jre^>
echo     ^<path^>jre^</path^>
echo     ^<requiresJdk^>false^</requiresJdk^>
echo     ^<requires64Bit^>false^</requires64Bit^>
echo     ^<minVersion^>21^</minVersion^>
echo     ^<maxVersion^>^</maxVersion^>
echo     ^<opt^>-Xmx2048m^</opt^>
echo     ^<opt^>-Xms256m^</opt^>
echo     ^<opt^>-XX:+UseG1GC^</opt^>
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
) >> "%CONFIG_TEMP%"

REM Generar EXE
echo Generando EXE...
"%LAUNCH4J%" "%CONFIG_TEMP%"

REM Verificar resultado
if exist "%DIST_DIR%\CONNECTING-POS.exe" (
    echo.
    echo ========================================
    echo EXE generado exitosamente!
    echo ========================================
    echo.
    for %%F in ("%DIST_DIR%\CONNECTING-POS.exe") do (
        set /a SIZE_MB=%%~zF/1048576
        set /a SIZE_KB=%%~zF/1024
        echo Tamaño: %%~zF bytes (~%SIZE_MB% MB / %SIZE_KB% KB^)
        
        if %SIZE_MB% GTR 130 (
            echo.
            echo ADVERTENCIA: El EXE excede 130 MB (actual: %SIZE_MB% MB^)
            echo Considera optimizar el JAR o eliminar dependencias innecesarias.
        ) else (
            echo.
            echo ✓ El EXE está dentro del límite de 130 MB
        )
    )
    echo.
    echo Ubicación: %DIST_DIR%\CONNECTING-POS.exe
    echo.
    
    REM Limpiar archivo temporal
    del "%CONFIG_TEMP%" 2>nul
) else (
    echo.
    echo ERROR: No se pudo generar el EXE
    echo Verifica que Launch4j esté correctamente instalado
    echo y que el archivo de configuración sea válido.
    echo.
    echo Configuración temporal guardada en: %CONFIG_TEMP%
    echo.
)

pause
