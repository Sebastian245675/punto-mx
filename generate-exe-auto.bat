@echo off
echo ========================================
echo Generando EXE optimizado (menos de 120 MB)
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

echo [OK] JAR encontrado: kriolos-opos-app\target\kriolos-pos.jar
echo.

REM Buscar Launch4j
set LAUNCH4J=
if exist "C:\Program Files\Launch4j\launch4j.exe" (
    set LAUNCH4J=C:\Program Files\Launch4j\launch4j.exe
    goto :found
)
if exist "C:\Program Files (x86)\Launch4j\launch4j.exe" (
    set LAUNCH4J=C:\Program Files (x86)\Launch4j\launch4j.exe
    goto :found
)
if exist "launch4j\launch4j.exe" (
    set LAUNCH4J=%CD%\launch4j\launch4j.exe
    goto :found
)

REM Descargar Launch4j si no existe
echo Launch4j no encontrado. Descargando...
if not exist "launch4j" mkdir launch4j

powershell -ExecutionPolicy Bypass -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $url = 'https://sourceforge.net/projects/launch4j/files/launch4j-3/3.50/launch4j-3.50-windows-x64.zip/download'; $out = 'launch4j\launch4j.zip'; Write-Host 'Descargando Launch4j...'; Invoke-WebRequest -Uri $url -OutFile $out -UseBasicParsing; Write-Host 'Extrayendo...'; Expand-Archive -Path $out -DestinationPath 'launch4j' -Force; Remove-Item $out}"

REM Buscar el ejecutable usando PowerShell
for /f "delims=" %%F in ('powershell -Command "Get-ChildItem -Path 'launch4j' -Filter 'launch4j.exe' -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName"') do (
    set LAUNCH4J=%%F
    goto :found
)

echo ERROR: No se pudo descargar o encontrar Launch4j
pause
exit /b 1

:found
echo [OK] Usando Launch4j: %LAUNCH4J%
echo.

REM Crear directorio dist
if not exist "dist" mkdir dist

REM Actualizar configuracion para no empaquetar JRE
echo Actualizando configuracion de Launch4j...
powershell -Command "(Get-Content 'launch4j-config.xml') -replace '<path>jre</path>', '<path></path>' -replace '<requires64Bit>false</requires64Bit>', '<bundledJre64Bit>false</bundledJre64Bit><bundledJreAsFallback>false</bundledJreAsFallback><runtimeBits>64/32</runtimeBits>' | Set-Content 'launch4j-config-temp.xml'"

REM Generar EXE
echo Generando EXE...
"%LAUNCH4J%" launch4j-config-temp.xml

if exist "dist\CONNECTING-POS.exe" (
    echo.
    echo ========================================
    echo [OK] EXE generado exitosamente!
    echo ========================================
    echo.
    for %%F in ("dist\CONNECTING-POS.exe") do (
        set /a SIZE_MB=%%~zF/1048576
        echo Tamaño: %%~zF bytes (~%SIZE_MB% MB^)
        if %SIZE_MB% LSS 120 (
            echo [OK] El EXE es menor a 120 MB - Objetivo cumplido!
        ) else (
            echo [ADVERTENCIA] El EXE es mayor a 120 MB
        )
    )
    echo.
    echo Ubicación: %CD%\dist\CONNECTING-POS.exe
    echo.
    echo NOTA: Este EXE requiere Java 21+ instalado en el sistema.
    echo.
    del launch4j-config-temp.xml 2>nul
) else (
    echo ERROR: No se pudo generar el EXE
    echo Verifica los logs de Launch4j
    del launch4j-config-temp.xml 2>nul
)

pause

