# Crear un ejecutable .exe ÚNICO y SIMPLE
# Usa jpackage para crear un ejecutable que funcione

$ErrorActionPreference = "Stop"

Write-Host "========================================"
Write-Host "Creando ejecutable .exe ÚNICO"
Write-Host "========================================"
Write-Host ""

$jarPath = "kriolos-opos-app\target\kriolos-pos.jar"
if (-not (Test-Path $jarPath)) {
    Write-Host "ERROR: JAR no encontrado: $jarPath"
    exit 1
}

$outputDir = "D:\descargas"
if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

# Copiar JAR a descargas
Write-Host "Copiando JAR..."
Copy-Item $jarPath "$outputDir\kriolos-pos.jar" -Force

# Crear launcher .bat optimizado
$batContent = @"
@echo off
cd /d "%~dp0"
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java no encontrado
    echo Instala Java 21+ desde: https://adoptium.net/
    pause
    exit /b 1
)
start "" /min java -Xms256m -Xmx2g -XX:+UseG1GC -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true -Xverify:none -XX:TieredStopAtLevel=1 -Dfile.encoding=UTF-8 -jar "kriolos-pos.jar"
"@

$batContent | Out-File -FilePath "$outputDir\CONNECTING-POS.bat" -Encoding ASCII -Force

Write-Host ""
Write-Host "========================================"
Write-Host "✅ LAUNCHER CREADO"
Write-Host "========================================"
Write-Host ""
Write-Host "Ubicación: $outputDir"
Write-Host "Archivos:"
Write-Host "  - CONNECTING-POS.bat (launcher)"
Write-Host "  - kriolos-pos.jar (~120 MB)"
Write-Host ""
Write-Host "Para convertir a .exe:"
Write-Host "  Usa: Bat To Exe Converter"
Write-Host "  https://www.battoexeconverter.com/"
Write-Host ""
Write-Host "O ejecuta directamente: CONNECTING-POS.bat"
Write-Host ""

