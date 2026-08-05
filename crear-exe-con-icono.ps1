# Script para generar EXE con icono (maximo 130 MB)
# Requiere Launch4j instalado

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Generando EXE con icono (max 130 MB)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar JAR
$jarPath = "kriolos-opos-app\target\kriolos-pos.jar"
if (-not (Test-Path $jarPath)) {
    Write-Host "ERROR: JAR no encontrado. Compilando..." -ForegroundColor Red
    mvn clean package -DskipTests -f pom.xml
    if (-not (Test-Path $jarPath)) {
        Write-Host "ERROR: No se pudo compilar el JAR" -ForegroundColor Red
        exit 1
    }
}

# Verificar tamano del JAR
$jarSize = (Get-Item $jarPath).Length
$jarSizeMB = [math]::Round($jarSize / 1MB, 2)
Write-Host "JAR encontrado: $([math]::Round($jarSize / 1MB, 2)) MB" -ForegroundColor Green

if ($jarSizeMB -gt 120) {
    Write-Host "ADVERTENCIA: El JAR es muy grande (>120 MB). El EXE podria exceder 130 MB." -ForegroundColor Yellow
}
Write-Host ""

# Buscar Launch4j
$launch4jPaths = @(
    "C:\Program Files\Launch4j\launch4j.exe",
    "C:\Program Files (x86)\Launch4j\launch4j.exe",
    "$PSScriptRoot\launch4j\launch4j.exe"
)

$launch4j = $null
foreach ($path in $launch4jPaths) {
    if (Test-Path $path) {
        $launch4j = $path
        break
    }
}

if (-not $launch4j) {
    Write-Host "ERROR: Launch4j no encontrado." -ForegroundColor Red
    Write-Host "Por favor, descarga Launch4j desde: https://launch4j.sourceforge.net/" -ForegroundColor Yellow
    Write-Host "O instala Launch4j y vuelve a ejecutar este script." -ForegroundColor Yellow
    exit 1
}

Write-Host "Usando Launch4j: $launch4j" -ForegroundColor Green
Write-Host ""

# Verificar si se debe omitir el icono (si existe icono.ico pero es invalido)
$skipIcon = $false
if (Test-Path "icono.ico") {
    try {
        Add-Type -AssemblyName System.Drawing
        $testIcon = New-Object System.Drawing.Icon("icono.ico")
        $testIcon.Dispose()
    } catch {
        Write-Host "ADVERTENCIA: El archivo icono.ico existe pero no es valido" -ForegroundColor Yellow
        Write-Host "Eliminando icono invalido..." -ForegroundColor Yellow
        Remove-Item "icono.ico" -Force -ErrorAction SilentlyContinue
        $skipIcon = $true
    }
}

# Buscar archivo de icono
$iconPaths = @(
    "icono.ico",
    "icon.ico",
    "resources\icono.ico",
    "kriolos-opos-app\src\main\resources\icono.ico"
)

$iconFile = $null
if (-not $skipIcon) {
    foreach ($path in $iconPaths) {
        if (Test-Path $path) {
            $iconFile = (Resolve-Path $path).Path
            break
        }
    }
}

# Si no hay ICO, buscar PNG y sugerir conversion
if (-not $iconFile -and -not $skipIcon) {
    $pngFiles = @("new_pos_icon.png", "icono.png", "icon.png")
    foreach ($png in $pngFiles) {
        if (Test-Path $png) {
            Write-Host "PNG encontrado: $png" -ForegroundColor Yellow
            Write-Host "Convirtiendo PNG a ICO..." -ForegroundColor Yellow
            
            # Intentar convertir usando PowerShell
            try {
                Add-Type -AssemblyName System.Drawing
                $bitmap = New-Object System.Drawing.Bitmap($png)
                $hIcon = $bitmap.GetHicon()
                $icon = [System.Drawing.Icon]::FromHandle($hIcon)
                $icoPath = "icono.ico"
                $fs = New-Object System.IO.FileStream($icoPath, [System.IO.FileMode]::Create)
                $icon.Save($fs)
                $fs.Close()
                $icon.Dispose()
                $bitmap.Dispose()
                
                if (Test-Path $icoPath) {
                    $iconFile = (Resolve-Path $icoPath).Path
                    Write-Host "Icono creado desde PNG: $iconFile" -ForegroundColor Green
                    break
                }
            } catch {
                Write-Host "No se pudo convertir automaticamente." -ForegroundColor Yellow
                Write-Host "Para crear un icono valido, ejecuta: .\convertir-png-a-ico.ps1" -ForegroundColor Yellow
                Write-Host "O descarga un icono .ico desde: https://www.flaticon.com/" -ForegroundColor Yellow
            }
        }
    }
}

if (-not $iconFile) {
    Write-Host "ADVERTENCIA: No se encontro archivo de icono (.ico)" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Para agregar un icono:" -ForegroundColor Yellow
    Write-Host "1. Coloca un archivo 'icono.ico' en la raiz del proyecto" -ForegroundColor Yellow
    Write-Host "2. O descarga uno desde: https://www.flaticon.com/" -ForegroundColor Yellow
    Write-Host "3. O crea uno usando: https://www.favicon-generator.org/" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Continuando sin icono..." -ForegroundColor Yellow
} else {
    Write-Host "Icono encontrado: $iconFile" -ForegroundColor Green
    
    # Verificar que el icono sea valido
    $iconValid = $false
    try {
        Add-Type -AssemblyName System.Drawing
        $testIcon = New-Object System.Drawing.Icon($iconFile)
        $testIcon.Dispose()
        $iconValid = $true
        Write-Host "Icono valido" -ForegroundColor Green
    } catch {
        Write-Host "ADVERTENCIA: El archivo de icono no es valido: $($_.Exception.Message)" -ForegroundColor Yellow
        Write-Host "Eliminando icono invalido y continuando sin icono..." -ForegroundColor Yellow
        Remove-Item $iconFile -Force -ErrorAction SilentlyContinue
        $iconFile = $null
    }
}
Write-Host ""

# Crear directorio de salida
$distDir = "D:\Descargas"
if (-not (Test-Path $distDir)) {
    $distDir = "D:\Downloads"
    if (-not (Test-Path $distDir)) {
        $distDir = Join-Path $PSScriptRoot "dist"
    }
}

if (-not (Test-Path $distDir)) {
    New-Item -ItemType Directory -Path $distDir -Force | Out-Null
}

Write-Host "Directorio de salida: $distDir" -ForegroundColor Green
Write-Host ""

# Obtener ruta absoluta del JAR
$jarPathAbs = (Resolve-Path $jarPath).Path
$outFile = Join-Path $distDir "CONNECTING-POS.exe"

# Crear configuracion temporal de Launch4j
$configTemp = Join-Path $env:TEMP "launch4j-config-temp.xml"

# Obtener ruta absoluta del icono si existe y es valido
$iconPathXml = ""
if ($iconFile) {
    # Validar que el icono sea realmente valido
    $iconValid = $false
    try {
        Add-Type -AssemblyName System.Drawing
        $testIcon = New-Object System.Drawing.Icon($iconFile)
        $testIcon.Dispose()
        $iconValid = $true
    } catch {
        Write-Host "ADVERTENCIA: El icono no es valido, se generara sin icono" -ForegroundColor Yellow
        $iconFile = $null
    }
    
    if ($iconValid) {
        $iconPathXml = (Resolve-Path $iconFile).Path
        Write-Host "Usando icono: $iconPathXml" -ForegroundColor Cyan
    }
}

if (-not $iconPathXml) {
    Write-Host "No se usara icono (campo vacio en config)" -ForegroundColor Yellow
}
Write-Host ""

# Escapar rutas para XML
$jarPathEscaped = $jarPathAbs -replace '&', '&amp;' -replace '<', '&lt;' -replace '>', '&gt;' -replace '"', '&quot;' -replace "'", '&apos;'
$outFileEscaped = $outFile -replace '&', '&amp;' -replace '<', '&lt;' -replace '>', '&gt;' -replace '"', '&quot;' -replace "'", '&apos;'
$iconPathEscaped = ""
if ($iconPathXml) {
    $iconPathEscaped = $iconPathXml -replace '&', '&amp;' -replace '<', '&lt;' -replace '>', '&gt;' -replace '"', '&quot;' -replace "'", '&apos;'
}

$configContent = @"
<?xml version="1.0" encoding="UTF-8"?>
<launch4jConfig>
  <dontWrapJar>false</dontWrapJar>
  <headerType>gui</headerType>
  <jar>$jarPathEscaped</jar>
  <outfile>$outFileEscaped</outfile>
  <errTitle>CONNECTING POS Error</errTitle>
  <cmdLine></cmdLine>
  <chdir>.</chdir>
  <priority>normal</priority>
  <downloadUrl>https://adoptium.net/</downloadUrl>
  <supportUrl>https://github.com/Sebastian245675/punto-mx</supportUrl>
  <stayAlive>false</stayAlive>
  <restartOnCrash>false</restartOnCrash>
  <manifest></manifest>
  <icon>$iconPathEscaped</icon>
  <jre>
    <path>jre</path>
    <requiresJdk>false</requiresJdk>
    <requires64Bit>false</requires64Bit>
    <minVersion>21</minVersion>
    <maxVersion></maxVersion>
    <opt>-Xmx2048m</opt>
    <opt>-Xms256m</opt>
    <opt>-XX:+UseG1GC</opt>
    <opt>-Dsun.java2d.uiScale=1.0</opt>
    <opt>-Djava.util.logging.config.file=logging.properties</opt>
  </jre>
  <versionInfo>
    <fileVersion>1.0.0.0</fileVersion>
    <txtFileVersion>1.0.0</txtFileVersion>
    <fileDescription>CONNECTING POS - Sistema de Punto de Venta</fileDescription>
    <copyright>2025 CONNECTING POS</copyright>
    <productVersion>1.0.0.0</productVersion>
    <txtProductVersion>1.0.0</txtProductVersion>
    <productName>CONNECTING POS</productName>
    <companyName>CONNECTING POS</companyName>
    <internalName>CONNECTING-POS</internalName>
    <originalFilename>CONNECTING-POS.exe</originalFilename>
  </versionInfo>
</launch4jConfig>
"@

$configContent | Out-File -FilePath $configTemp -Encoding UTF8

# Eliminar EXE anterior si existe para forzar regeneracion
if (Test-Path $outFile) {
    Write-Host "Eliminando EXE anterior..." -ForegroundColor Yellow
    Remove-Item $outFile -Force -ErrorAction SilentlyContinue
}

# Mostrar configuracion antes de generar
Write-Host "Configuracion:" -ForegroundColor Cyan
Write-Host "  JAR: $jarPathAbs" -ForegroundColor Gray
Write-Host "  Salida: $outFile" -ForegroundColor Gray
if ($iconPathXml) {
    Write-Host "  Icono: $iconPathXml" -ForegroundColor Gray
} else {
    Write-Host "  Icono: (ninguno)" -ForegroundColor Gray
}
Write-Host ""

# Verificar que el XML sea valido antes de ejecutar
Write-Host "Verificando configuracion XML..." -ForegroundColor Cyan
try {
    [xml]$null = Get-Content $configTemp
    Write-Host "XML valido" -ForegroundColor Green
} catch {
    Write-Host "ERROR: El XML de configuracion no es valido: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Archivo: $configTemp" -ForegroundColor Yellow
    exit 1
}
Write-Host ""

# Generar EXE
Write-Host "Generando EXE..." -ForegroundColor Yellow

# Ejecutar Launch4j y capturar salida
$processInfo = New-Object System.Diagnostics.ProcessStartInfo
$processInfo.FileName = $launch4j
$processInfo.Arguments = "`"$configTemp`""
$processInfo.UseShellExecute = $false
$processInfo.RedirectStandardOutput = $true
$processInfo.RedirectStandardError = $true
$processInfo.CreateNoWindow = $true

$process = New-Object System.Diagnostics.Process
$process.StartInfo = $processInfo

try {
    $process.Start() | Out-Null
    $output = $process.StandardOutput.ReadToEnd()
    $errorOutput = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    
    if ($process.ExitCode -ne 0) {
        Write-Host "Error al ejecutar Launch4j (codigo: $($process.ExitCode))" -ForegroundColor Red
        if ($output) {
            Write-Host "Salida: $output" -ForegroundColor Red
        }
        if ($errorOutput) {
            Write-Host "Errores: $errorOutput" -ForegroundColor Red
        }
    } elseif ($output) {
        Write-Host $output -ForegroundColor Gray
    }
} catch {
    Write-Host "ERROR al ejecutar Launch4j: $($_.Exception.Message)" -ForegroundColor Red
}

# Verificar resultado
if (Test-Path $outFile) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "EXE generado exitosamente!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    
    $exeSize = (Get-Item $outFile).Length
    $exeSizeMB = [math]::Round($exeSize / 1MB, 2)
    $exeSizeKB = [math]::Round($exeSize / 1KB, 2)
    
    Write-Host "Tamano: $exeSize bytes (~$exeSizeMB MB / $exeSizeKB KB)" -ForegroundColor Cyan
    
    if ($exeSizeMB -gt 130) {
        Write-Host ""
        Write-Host "ADVERTENCIA: El EXE excede 130 MB (actual: $exeSizeMB MB)" -ForegroundColor Red
        Write-Host "Considera optimizar el JAR o eliminar dependencias innecesarias." -ForegroundColor Yellow
    } else {
        Write-Host ""
        Write-Host "El EXE esta dentro del limite de 130 MB" -ForegroundColor Green
    }
    
    Write-Host ""
    Write-Host "Ubicacion: $outFile" -ForegroundColor Cyan
    Write-Host ""
    
    # Limpiar archivo temporal
    Remove-Item $configTemp -ErrorAction SilentlyContinue
} else {
    Write-Host ""
    Write-Host "ERROR: No se pudo generar el EXE" -ForegroundColor Red
    Write-Host ""
    Write-Host "Posibles causas:" -ForegroundColor Yellow
    Write-Host "1. El icono no es valido - Intenta generar sin icono o usa otro icono" -ForegroundColor Yellow
    Write-Host "2. Launch4j tiene un problema - Verifica la instalacion" -ForegroundColor Yellow
    Write-Host "3. El JAR esta corrupto o no es accesible" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Configuracion temporal guardada en: $configTemp" -ForegroundColor Yellow
    Write-Host "Puedes abrir este archivo con Launch4j GUI para ver el error detallado" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Para generar sin icono, elimina el archivo icono.ico y vuelve a ejecutar" -ForegroundColor Cyan
    Write-Host ""
}
