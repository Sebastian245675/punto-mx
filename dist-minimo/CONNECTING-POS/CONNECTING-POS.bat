@echo off
REM Launcher para CONNECTING POS
setlocal

REM Buscar Java
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java no encontrado. Por favor instala Java 21 o superior.
    echo Descarga desde: https://adoptium.net/
    pause
    exit /b 1
)

REM Ejecutar aplicación
java -Xms256m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true -Xverify:none -XX:TieredStopAtLevel=1 -XX:+TieredCompilation -Dfile.encoding=UTF-8 -jar "%~dp0kriolos-pos.jar"

