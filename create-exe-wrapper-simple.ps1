# Crear un ejecutable .exe wrapper simple usando IExpress (incluido en Windows)
# Esto crea un .exe que ejecuta el JAR sin incluir el JRE

$ErrorActionPreference = "Stop"

Write-Host "Creando ejecutable wrapper simple..."

$jarPath = "kriolos-opos-app\target\kriolos-pos.jar"
if (-not (Test-Path $jarPath)) {
    Write-Host "ERROR: JAR no encontrado"
    exit 1
}

$outputDir = "D:\descargas\CONNECTING-POS-Lite"
if (Test-Path $outputDir) {
    Remove-Item $outputDir -Recurse -Force
}
New-Item -ItemType Directory -Path $outputDir -Force | Out-Null

# Copiar solo el JAR
Copy-Item $jarPath "$outputDir\kriolos-pos.jar"

# Crear un script VBS que ejecuta el JAR (se puede convertir a .exe fácilmente)
$vbsScript = @"
Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

' Obtener directorio del script
scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)
jarPath = scriptDir & "\kriolos-pos.jar"

' Verificar Java
On Error Resume Next
Set javaProc = WshShell.Exec("java -version")
javaProc.StdOut.ReadAll
javaProc.StdErr.ReadAll
If Err.Number <> 0 Then
    MsgBox "Java no encontrado. Por favor instala Java 21+ desde https://adoptium.net/", vbCritical, "CONNECTING POS - Error"
    WScript.Quit
End If
On Error GoTo 0

' Ejecutar aplicación
WshShell.CurrentDirectory = scriptDir
WshShell.Run "java -Xms256m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true -Xverify:none -XX:TieredStopAtLevel=1 -XX:+TieredCompilation -Dfile.encoding=UTF-8 -jar """ & jarPath & """", 0, False
"@

$vbsScript | Out-File -FilePath "$outputDir\CONNECTING-POS.vbs" -Encoding ASCII

# Crear también el .bat
$batContent = @"
@echo off
cd /d "%~dp0"
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java no encontrado
    echo Por favor instala Java 21+ desde: https://adoptium.net/
    pause
    exit /b 1
)
java -Xms256m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true -Xverify:none -XX:TieredStopAtLevel=1 -XX:+TieredCompilation -Dfile.encoding=UTF-8 -jar "kriolos-pos.jar"
"@

$batContent | Out-File -FilePath "$outputDir\CONNECTING-POS.bat" -Encoding ASCII

Write-Host ""
Write-Host "========================================"
Write-Host "Versión LITE creada en: $outputDir"
Write-Host "========================================"
Write-Host ""
Write-Host "Archivos:"
Write-Host "  - kriolos-pos.jar (~120 MB)"
Write-Host "  - CONNECTING-POS.bat (launcher)"
Write-Host "  - CONNECTING-POS.vbs (launcher VBS)"
Write-Host ""
Write-Host "Tamaño total: ~120 MB"
Write-Host ""
Write-Host "Para convertir .bat a .exe, usa:"
Write-Host "  - Bat To Exe Converter: https://www.battoexeconverter.com/"
Write-Host "  - O simplemente usa el .bat directamente"
Write-Host ""

