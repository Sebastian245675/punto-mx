@echo off
echo ========================================
echo Instalando Launch4j y generando EXE
echo ========================================
echo.

REM Verificar JAR
if not exist "kriolos-opos-app\target\kriolos-pos.jar" (
    echo ERROR: JAR no encontrado
    pause
    exit /b 1
)

echo [OK] JAR encontrado
echo.

REM Crear carpeta para Launch4j
if not exist "launch4j-temp" mkdir launch4j-temp

echo Descargando Launch4j...
echo Esto puede tardar unos minutos...
echo.

REM Descargar usando PowerShell
powershell -ExecutionPolicy Bypass -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $ProgressPreference = 'SilentlyContinue'; $url = 'https://sourceforge.net/projects/launch4j/files/launch4j-3/3.50/launch4j-3.50-windows-x64.exe/download'; $out = 'launch4j-temp\launch4j-installer.exe'; Write-Host 'Descargando desde SourceForge...'; try { Invoke-WebRequest -Uri $url -OutFile $out -UseBasicParsing -ErrorAction Stop; Write-Host 'Descarga completada' -ForegroundColor Green; Write-Host 'Instalando Launch4j...'; Start-Process -FilePath $out -ArgumentList '/S' -Wait; Write-Host 'Instalacion completada' -ForegroundColor Green } catch { Write-Host 'Error al descargar: ' $_.Exception.Message -ForegroundColor Red; exit 1 } }"

REM Buscar Launch4j instalado
set LAUNCH4J=
if exist "C:\Program Files\Launch4j\launch4j.exe" set LAUNCH4J=C:\Program Files\Launch4j\launch4j.exe
if exist "C:\Program Files (x86)\Launch4j\launch4j.exe" set LAUNCH4J=C:\Program Files (x86)\Launch4j\launch4j.exe

if "%LAUNCH4J%"=="" (
    echo.
    echo ERROR: Launch4j no se instalo correctamente
    echo Por favor, instala Launch4j manualmente desde:
    echo https://launch4j.sourceforge.net/
    echo.
    pause
    exit /b 1
)

echo [OK] Launch4j instalado: %LAUNCH4J%
echo.

REM Limpiar archivo temporal
if exist "launch4j-temp" rmdir /s /q launch4j-temp

REM Generar EXE
echo Generando EXE en D:\Descargas...
echo.

powershell -Command "$config = Get-Content 'launch4j-config.xml' -Raw; $config = $config -replace '<outfile>.*?</outfile>', '<outfile>D:\Descargas\CONNECTING-POS.exe</outfile>'; $config | Out-File 'launch4j-config-temp.xml' -Encoding UTF8"

"%LAUNCH4J%" launch4j-config-temp.xml

if exist "D:\Descargas\CONNECTING-POS.exe" (
    echo.
    echo ========================================
    echo [OK] EXE generado exitosamente!
    echo ========================================
    echo.
    echo Ubicacion: D:\Descargas\CONNECTING-POS.exe
    for %%F in ("D:\Descargas\CONNECTING-POS.exe") do (
        set /a SIZE_MB=%%~zF/1048576
        echo Tamaño: ~%SIZE_MB% MB
    )
    echo.
    start "" "D:\Descargas"
    del launch4j-config-temp.xml 2>nul
) else (
    echo ERROR: No se pudo generar el EXE
    del launch4j-config-temp.xml 2>nul
)

pause















