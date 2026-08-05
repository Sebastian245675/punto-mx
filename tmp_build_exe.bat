@echo off
setlocal enabledelayedexpansion

REM Buscar Java
if exist "C:\Program Files\Java\jdk-25.0.2" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.2"
) else if exist "C:\Program Files\Java\jdk-25" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-25"
) else if exist "C:\Program Files\Java\jdk-21" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-21"
)

if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    echo JAVA_HOME set to: %JAVA_HOME%
)

echo Compilando con Maven Wrapper...
call .\mvnw.cmd clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo ERROR en la compilacion.
    exit /b %ERRORLEVEL%
)

echo Generando EXE con el script de Descargas...
call generar-exe-ahora.bat
