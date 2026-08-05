# Script para generar ejecutable .exe completo en D:\Downloads
# Incluye el JAR y el ejecutable, tamaño total < 150 MB

$ErrorActionPreference = "Stop"

Write-Host "========================================"
Write-Host "Generando ejecutable .exe completo"
Write-Host "========================================"
Write-Host ""

# Verificar JAR
$jarPath = "kriolos-opos-app\target\kriolos-pos.jar"
if (-not (Test-Path $jarPath)) {
    Write-Host "ERROR: No se encuentra el JAR: $jarPath"
    Write-Host "Compilando proyecto..."
    mvn clean package -DskipTests -f pom.xml
    if (-not (Test-Path $jarPath)) {
        Write-Host "ERROR: No se pudo compilar el JAR"
        exit 1
    }
}

$jarSize = (Get-Item $jarPath).Length / 1MB
Write-Host "JAR encontrado: $([math]::Round($jarSize, 2)) MB"
Write-Host ""

# Directorio de salida
$outputDir = "D:\Downloads"
if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

# Crear carpeta para la aplicación
$appFolder = Join-Path $outputDir "CONNECTING-POS"
if (Test-Path $appFolder) {
    Remove-Item $appFolder -Recurse -Force
}
New-Item -ItemType Directory -Path $appFolder -Force | Out-Null

# Copiar JAR
Write-Host "Copiando JAR a D:\Downloads\CONNECTING-POS..."
Copy-Item $jarPath "$appFolder\kriolos-pos.jar" -Force

# Crear script PowerShell que se puede convertir a .exe
$psScriptContent = @'
# Launcher para CONNECTING POS
$jarPath = Join-Path $PSScriptRoot "kriolos-pos.jar"

# Verificar Java
try {
    $javaVersion = java -version 2>&1
    if ($LASTEXITCODE -ne 0) {
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
Set-Location $PSScriptRoot
java -Xms256m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true -Xverify:none -XX:TieredStopAtLevel=1 -XX:+TieredCompilation -Dfile.encoding=UTF-8 -jar "kriolos-pos.jar"
'@

$psScriptPath = Join-Path $appFolder "CONNECTING-POS.ps1"
$psScriptContent | Out-File -FilePath $psScriptPath -Encoding UTF8

# Intentar convertir a .exe con jpackage
Write-Host "Creando ejecutable con jpackage..."
$tempAppDir = Join-Path $env:TEMP "kriolos-pos-temp"
if (Test-Path $tempAppDir) {
    Remove-Item $tempAppDir -Recurse -Force
}
New-Item -ItemType Directory -Path $tempAppDir -Force | Out-Null
Copy-Item $jarPath "$tempAppDir\kriolos-pos.jar"

try {
    $jpackageOutput = Join-Path $env:TEMP "jpackage-output"
    if (Test-Path $jpackageOutput) {
        Remove-Item $jpackageOutput -Recurse -Force
    }
    
    jpackage --input $tempAppDir --name "CONNECTING-POS" --main-jar "kriolos-pos.jar" --type app-image --dest $jpackageOutput --java-options "-Xms256m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true -Xverify:none -XX:TieredStopAtLevel=1 -XX:+TieredCompilation -Dfile.encoding=UTF-8" --app-version "1.0.0" --description "Sistema de Punto de Venta CONNECTING POS" --vendor "CONNECTING POS" 2>&1 | Out-Null
    
    $jpackageExe = Join-Path $jpackageOutput "CONNECTING-POS\CONNECTING-POS.exe"
    if (Test-Path $jpackageExe) {
        Copy-Item $jpackageExe "$appFolder\CONNECTING-POS.exe" -Force
        Write-Host "Ejecutable creado exitosamente"
    }
    Remove-Item $jpackageOutput -Recurse -Force -ErrorAction SilentlyContinue
} catch {
    Write-Host "No se pudo crear .exe con jpackage, se creó solo el script .ps1"
}

# Limpiar
Remove-Item $tempAppDir -Recurse -Force -ErrorAction SilentlyContinue

# Calcular tamaño total
$totalSize = (Get-ChildItem $appFolder -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB
$exePath = Join-Path $appFolder "CONNECTING-POS.exe"

Write-Host ""
Write-Host "========================================"
Write-Host "Aplicación creada exitosamente!"
Write-Host "========================================"
Write-Host ""
Write-Host "Ubicación: $appFolder"
if (Test-Path $exePath) {
    $exeSize = (Get-Item $exePath).Length / 1MB
    Write-Host "Ejecutable: CONNECTING-POS.exe ($([math]::Round($exeSize, 2)) MB)"
}
Write-Host "JAR: kriolos-pos.jar ($([math]::Round($jarSize, 2)) MB)"
Write-Host "Tamaño total: $([math]::Round($totalSize, 2)) MB"
Write-Host ""

if ($totalSize -gt 150) {
    Write-Host "ADVERTENCIA: El tamaño total excede 150 MB"
} else {
    Write-Host "Tamaño dentro del límite de 150 MB"
}

Write-Host ""
Write-Host "Para ejecutar:"
if (Test-Path $exePath) {
    Write-Host "  - Doble clic en: $exePath"
} else {
    Write-Host "  - Ejecuta: $psScriptPath"
    Write-Host "  - O convierte el .ps1 a .exe usando PS2EXE o Bat To Exe Converter"
}
Write-Host ""
Write-Host "NOTA: Requiere Java 21+ instalado en el sistema"
Write-Host ""















