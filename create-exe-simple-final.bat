@echo off
REM Crear un ejecutable .exe SIMPLE, LIGERO y FUNCIONAL
REM Usa el JAR directamente sin copiar carpetas

echo ========================================
echo Creando ejecutable SIMPLE y LIGERO
echo ========================================
echo.

set APP_JAR=kriolos-opos-app\target\kriolos-pos.jar
if not exist "%APP_JAR%" (
    echo ERROR: JAR no encontrado
    pause
    exit /b 1
)

REM Crear directorio de salida
if not exist "D:\descargas" mkdir "D:\descargas"
if exist "D:\descargas\CONNECTING-POS-Simple.exe" del "D:\descargas\CONNECTING-POS-Simple.exe"

echo Creando launcher optimizado...

REM Crear un script VBS que ejecuta el JAR
REM Este se puede convertir a .exe fácilmente
(
echo Set WshShell = CreateObject^("WScript.Shell"^)
echo Set fso = CreateObject^("Scripting.FileSystemObject"^)
echo.
echo ' Obtener ruta del script
echo scriptDir = fso.GetParentFolderName^(WScript.ScriptFullName^)
echo jarPath = scriptDir ^& "\kriolos-pos.jar"
echo.
echo ' Verificar Java
echo On Error Resume Next
echo Set javaProc = WshShell.Exec^("java -version"^)
echo javaProc.StdOut.ReadAll
echo javaProc.StdErr.ReadAll
echo If Err.Number ^<^> 0 Then
echo     MsgBox "Java no encontrado. Instala Java 21+ desde https://adoptium.net/", vbCritical, "CONNECTING POS"
echo     WScript.Quit
echo End If
echo On Error GoTo 0
echo.
echo ' Ejecutar aplicación
echo WshShell.CurrentDirectory = scriptDir
echo WshShell.Run "java -Xms256m -Xmx2g -XX:+UseG1GC -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true -Xverify:none -XX:TieredStopAtLevel=1 -Dfile.encoding=UTF-8 -jar """ ^& jarPath ^& """", 0, False
) > "D:\descargas\CONNECTING-POS.vbs"

REM Copiar JAR junto al launcher
copy "%APP_JAR%" "D:\descargas\kriolos-pos.jar" >nul

echo.
echo ========================================
echo Ejecutable creado
echo ========================================
echo.
echo Ubicación: D:\descargas\
echo Archivos:
echo   - CONNECTING-POS.vbs (launcher)
echo   - kriolos-pos.jar (~120 MB)
echo.
echo Para convertir .vbs a .exe:
echo   1. Usa VBS To EXE Converter (gratis)
echo   2. O usa el .vbs directamente (doble clic)
echo.
echo Tamaño total: ~120 MB
echo.
echo IMPORTANTE: Requiere Java 21+ instalado
echo.

REM Intentar crear un .exe usando PowerShell si es posible
echo Intentando crear .exe con PowerShell...
powershell -Command "$jar = 'D:\descargas\kriolos-pos.jar'; $exe = 'D:\descargas\CONNECTING-POS-Simple.exe'; $content = @'
@echo off
cd /d \"%~dp0\"
where java >nul 2>&1
if %%ERRORLEVEL%% NEQ 0 ^(
    echo ERROR: Java no encontrado
    echo Instala Java 21+ desde: https://adoptium.net/
    pause
    exit /b 1
^)
start \"\" /min java -Xms256m -Xmx2g -XX:+UseG1GC -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true -Xverify:none -XX:TieredStopAtLevel=1 -Dfile.encoding=UTF-8 -jar \"kriolos-pos.jar\"
'@; Set-Content -Path $exe.Replace('.exe', '.bat') -Value $content -Encoding ASCII"

echo.
echo ========================================
echo LISTO
echo ========================================
echo.
echo Ejecuta: D:\descargas\CONNECTING-POS.vbs
echo O: D:\descargas\CONNECTING-POS-Simple.bat
echo.
pause

