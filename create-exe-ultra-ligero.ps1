# Script para crear ejecutable ULTRA LIGERO (~solo JAR)
# Crea un wrapper .exe mínimo que ejecuta el JAR

$ErrorActionPreference = "Stop"

Write-Host "========================================"
Write-Host "Creando ejecutable ULTRA LIGERO"
Write-Host "========================================"
Write-Host ""

$jarPath = "kriolos-opos-app\target\kriolos-pos.jar"
if (-not (Test-Path $jarPath)) {
    Write-Host "ERROR: No se encuentra el JAR: $jarPath"
    exit 1
}

$jarSize = (Get-Item $jarPath).Length / 1MB
Write-Host "Tamaño del JAR: $([math]::Round($jarSize, 2)) MB"
Write-Host ""

# Crear directorio de salida
$outputDir = "dist-ultra-ligero\CONNECTING-POS"
if (Test-Path $outputDir) {
    Remove-Item $outputDir -Recurse -Force
}
New-Item -ItemType Directory -Path $outputDir -Force | Out-Null

# Copiar solo el JAR
Write-Host "Copiando JAR..."
Copy-Item $jarPath "$outputDir\kriolos-pos.jar"

# Crear script PowerShell que se puede convertir a .exe
$psScript = @"
# Launcher para CONNECTING POS
`$jarPath = Join-Path `$PSScriptRoot "kriolos-pos.jar"

# Verificar Java
try {
    `$javaVersion = java -version 2>&1
    if (`$LASTEXITCODE -ne 0) {
        [System.Windows.Forms.MessageBox]::Show(
            "Java no encontrado.`n`nPor favor instala Java 21 o superior desde:`nhttps://adoptium.net/",
            "CONNECTING POS - Error",
            [System.Windows.Forms.MessageBoxButtons]::OK,
            [System.Windows.Forms.MessageBoxIcon]::Error
        )
        exit 1
    }
} catch {
    [System.Windows.Forms.MessageBox]::Show(
        "Java no encontrado.`n`nPor favor instala Java 21 o superior desde:`nhttps://adoptium.net/",
        "CONNECTING POS - Error",
        [System.Windows.Forms.MessageBoxButtons]::OK,
        [System.Windows.Forms.MessageBoxIcon]::Error
    )
    exit 1
}

# Ejecutar aplicación
Set-Location `$PSScriptRoot
java -Xms256m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true -Xverify:none -XX:TieredStopAtLevel=1 -XX:+TieredCompilation -Dfile.encoding=UTF-8 -jar "kriolos-pos.jar"
"@

$psScript | Out-File -FilePath "$outputDir\CONNECTING-POS.ps1" -Encoding UTF8

# Crear también un .bat como alternativa
$batScript = @"
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
"@

$batScript | Out-File -FilePath "$outputDir\CONNECTING-POS.bat" -Encoding ASCII

Write-Host ""
Write-Host "========================================"
Write-Host "Ejecutable ULTRA LIGERO creado"
Write-Host "========================================"
Write-Host ""
Write-Host "Ubicación: $outputDir"
Write-Host "Tamaño: ~$([math]::Round($jarSize, 2)) MB (solo JAR)"
Write-Host ""
Write-Host "Archivos creados:"
Write-Host "  - kriolos-pos.jar (~$([math]::Round($jarSize, 2)) MB)"
Write-Host "  - CONNECTING-POS.bat (launcher)"
Write-Host "  - CONNECTING-POS.ps1 (launcher PowerShell)"
Write-Host ""
Write-Host "Para convertir a .exe:"
Write-Host "  1. Usa PS2EXE: Install-Module ps2exe -Force"
Write-Host "     ps2exe CONNECTING-POS.ps1 CONNECTING-POS.exe"
Write-Host "  2. O usa Bat To Exe Converter para el .bat"
Write-Host "  3. O simplemente usa el .bat directamente"
Write-Host ""
Write-Host "NOTA: Requiere Java 21+ instalado en el sistema destino"
Write-Host ""

