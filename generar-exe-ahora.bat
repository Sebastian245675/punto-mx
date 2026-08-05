@echo off
echo ========================================
echo Generando EXE en D:\Descargas
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

REM Buscar Launch4j en varias ubicaciones
set LAUNCH4J=
set SEARCH_PATHS[0]=C:\Program Files\Launch4j\launch4j.exe
set SEARCH_PATHS[1]=C:\Program Files (x86)\Launch4j\launch4j.exe
set SEARCH_PATHS[2]=%CD%\launch4j\launch4j.exe
set SEARCH_PATHS[3]=%USERPROFILE%\AppData\Local\Launch4j\launch4j.exe
set SEARCH_PATHS[4]=%USERPROFILE%\Downloads\Launch4j\launch4j.exe

for /L %%i in (0,1,4) do (
    call set "TEST_PATH=%%SEARCH_PATHS[%%i]%%"
    if exist "!TEST_PATH!" (
        set LAUNCH4J=!TEST_PATH!
        goto :found
    )
)

REM Buscar recursivamente en launch4j si existe
if exist "launch4j" (
    for /r "launch4j" %%F in (launch4j.exe) do (
        set LAUNCH4J=%%F
        goto :found
    )
)

echo Launch4j no encontrado.
echo.
echo Por favor, descarga Launch4j desde:
echo https://sourceforge.net/projects/launch4j/files/launch4j-3/3.50/launch4j-3.50-windows-x64.exe/download
echo.
echo O desde la pagina oficial:
echo https://launch4j.sourceforge.net/
echo.
echo Despues de instalarlo o extraerlo, ejecuta este script nuevamente.
echo.
pause
exit /b 1

:found
echo [OK] Launch4j encontrado: %LAUNCH4J%
echo.

REM Asegurar que D:\Descargas existe
if not exist "D:\Descargas" (
    if exist "D:\Downloads" (
        set DIST_DIR=D:\Downloads
    ) else (
        mkdir D:\Descargas
        set DIST_DIR=D:\Descargas
    )
) else (
    set DIST_DIR=D:\Descargas
)

echo Generando EXE en: %DIST_DIR%
echo.

REM Crear configuracion temporal
powershell -Command "$config = Get-Content 'launch4j-config.xml' -Raw; $config = $config -replace '<outfile>.*?</outfile>', '<outfile>%DIST_DIR%\CONNECTING-POS.exe</outfile>'; $config | Out-File 'launch4j-config-temp.xml' -Encoding UTF8"

REM Generar EXE
"%LAUNCH4J%" launch4j-config-temp.xml

REM Verificar resultado
if exist "%DIST_DIR%\CONNECTING-POS.exe" (
    echo.
    echo ========================================
    echo [OK] EXE generado exitosamente!
    echo ========================================
    echo.
    echo Ubicacion: %DIST_DIR%\CONNECTING-POS.exe
    for %%F in ("%DIST_DIR%\CONNECTING-POS.exe") do (
        set /a SIZE_MB=%%~zF/1048576
        echo Tamaño: ~%SIZE_MB% MB
    )
    echo.
    echo Abriendo carpeta...
    start "" "%DIST_DIR%"
    del launch4j-config-temp.xml 2>nul
) else (
    echo.
    echo ERROR: No se pudo generar el EXE
    echo Verifica los logs de Launch4j
    del launch4j-config-temp.xml 2>nul
)

pause















