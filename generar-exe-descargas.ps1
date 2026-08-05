# Script para generar ejecutable .exe ligero en D:\Downloads
# Tamaño objetivo: < 150 MB

$ErrorActionPreference = "Stop"

Write-Host "========================================"
Write-Host "Generando ejecutable .exe ligero"
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

$exeName = "CONNECTING-POS.exe"
$exePath = Join-Path $outputDir $exeName

Write-Host "Creando ejecutable en: $exePath"
Write-Host ""

# Crear directorio temporal para la aplicación
$tempAppDir = Join-Path $env:TEMP "kriolos-pos-temp"
if (Test-Path $tempAppDir) {
    Remove-Item $tempAppDir -Recurse -Force
}
New-Item -ItemType Directory -Path $tempAppDir -Force | Out-Null

# Copiar JAR
Write-Host "Preparando archivos..."
Copy-Item $jarPath "$tempAppDir\kriolos-pos.jar"

# Crear ejecutable con jpackage (sin incluir runtime para mantener tamaño pequeño)
Write-Host "Ejecutando jpackage (esto puede tardar unos minutos)..."
Write-Host ""

try {
    jpackage --input $tempAppDir --name "CONNECTING-POS" --main-jar "kriolos-pos.jar" --type app-image --dest $outputDir --java-options "-Xms256m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true -Xverify:none -XX:TieredStopAtLevel=1 -XX:+TieredCompilation -Dfile.encoding=UTF-8" --app-version "1.0.0" --description "Sistema de Punto de Venta CONNECTING POS" --vendor "CONNECTING POS"
    
    # Mover el .exe a la ubicación final
    $jpackageExe = Join-Path $outputDir "CONNECTING-POS\CONNECTING-POS.exe"
    if (Test-Path $jpackageExe) {
        Move-Item $jpackageExe $exePath -Force
        Remove-Item (Join-Path $outputDir "CONNECTING-POS") -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "Ejecutable movido a: $exePath"
    }
} catch {
    Write-Host "ERROR con jpackage: $_"
    Write-Host ""
    Write-Host "Creando solución alternativa: wrapper simple..."
    
    # Crear wrapper .bat como alternativa
    $batContent = "@echo off`r`nsetlocal`r`ncd /d `"%~dp0`"`r`n`r`nwhere java >nul 2>&1`r`nif %ERRORLEVEL% NEQ 0 (`r`n    echo ERROR: Java no encontrado`r`n    echo Por favor instala Java 21 o superior desde: https://adoptium.net/`r`n    pause`r`n    exit /b 1`r`n)`r`n`r`njava -Xms256m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true -Xverify:none -XX:TieredStopAtLevel=1 -XX:+TieredCompilation -Dfile.encoding=UTF-8 -jar `"kriolos-pos.jar`"`r`n"
    
    # Copiar JAR
    Copy-Item $jarPath "$outputDir\kriolos-pos.jar" -Force
    
    # Crear .bat
    $batPath = Join-Path $outputDir "CONNECTING-POS.bat"
    [System.IO.File]::WriteAllText($batPath, $batContent)
    
    Write-Host ""
    Write-Host "Se creó un archivo .bat en: $batPath"
    Write-Host "Para convertirlo a .exe, puedes usar Bat To Exe Converter"
    Write-Host ""
}

# Limpiar
Remove-Item $tempAppDir -Recurse -Force -ErrorAction SilentlyContinue

# Verificar resultado
if (Test-Path $exePath) {
    $exeSize = (Get-Item $exePath).Length / 1MB
    Write-Host ""
    Write-Host "========================================"
    Write-Host "Ejecutable creado exitosamente!"
    Write-Host "========================================"
    Write-Host ""
    Write-Host "Ubicación: $exePath"
    Write-Host "Tamaño: $([math]::Round($exeSize, 2)) MB"
    Write-Host ""
    
    if ($exeSize -gt 150) {
        Write-Host "ADVERTENCIA: El ejecutable excede 150 MB"
    } else {
        Write-Host "Tamaño dentro del límite de 150 MB"
    }
    
    Write-Host ""
    Write-Host "NOTA: Este ejecutable requiere Java 21+ instalado en el sistema"
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "ERROR: No se pudo crear el ejecutable"
    Write-Host "Verifica que jpackage esté disponible o usa el .bat creado"
    Write-Host ""
    exit 1
}
