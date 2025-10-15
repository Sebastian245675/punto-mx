@echo off
REM Script de lanzamiento para SebastianPOS
REM Este script ejecuta el JAR de Spring Boot correctamente

cd /d "%~dp0app"

REM Verificar si existe runtime
if exist "..\runtime\bin\java.exe" (
    set "JAVA_EXE=..\runtime\bin\java.exe"
) else (
    set "JAVA_EXE=java"
)

REM Ejecutar el JAR de Spring Boot
"%JAVA_EXE%" -Xmx512m -Xms256m -Dfile.encoding=UTF-8 -jar "kriolos-pos.jar"

if errorlevel 1 (
    echo.
    echo ERROR: La aplicación no pudo iniciarse correctamente.
    echo Presione cualquier tecla para salir...
    pause > nul
)
