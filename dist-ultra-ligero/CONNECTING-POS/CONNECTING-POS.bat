@echo off
setlocal
cd /d "%~dp0"

where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java no encontrado
    echo Por favor instala Java 21 o superior desde: https://adoptium.net/
    pause
    exit /b 1
)

java -Xms256m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true -Xverify:none -XX:TieredStopAtLevel=1 -XX:+TieredCompilation -Dfile.encoding=UTF-8 -jar "kriolos-pos.jar"
