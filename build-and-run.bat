@echo off
echo ========================================
echo Configurando Java...
echo ========================================

REM Configurar JAVA_HOME para usar Java 25 (o buscar automaticamente)
if exist "C:\Program Files\Java\jdk-25" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-25"
    echo JAVA_HOME configurado a: %JAVA_HOME%
) else if exist "C:\Program Files\Java\jdk-21" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-21"
    echo JAVA_HOME configurado a: %JAVA_HOME%
) else (
    echo Advertencia: No se encontro JDK 25 o 21 en ubicaciones comunes
    echo Verificando JAVA_HOME actual: %JAVA_HOME%
)

REM Agregar Java al PATH
if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

java -version
echo.

echo ========================================
echo Compilando proyecto (saltando tests)...
echo ========================================
REM Intentar usar Maven Wrapper primero, si no existe usar mvn
if exist mvnw.cmd (
    call mvnw.cmd clean install -DskipTests
) else (
    call mvn clean install -DskipTests
)

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: La compilacion fallo
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ========================================
echo Ejecutando aplicacion...
echo ========================================
java -jar kriolos-opos-app\target\kriolos-pos.jar

pause

