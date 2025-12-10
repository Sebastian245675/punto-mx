# Script para generar EXE optimizado (menos de 120 MB)
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Generando EXE optimizado (menos de 120 MB)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar JAR
$jarPath = "kriolos-opos-app\target\kriolos-pos.jar"
if (-not (Test-Path $jarPath)) {
    Write-Host "ERROR: JAR no encontrado. Compilando..." -ForegroundColor Red
    & mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Fallo la compilacion" -ForegroundColor Red
        Read-Host "Presiona Enter para salir"
        exit 1
    }
}

$jarSize = (Get-Item $jarPath).Length / 1MB
Write-Host "[OK] JAR encontrado: $jarPath (~$([math]::Round($jarSize, 2)) MB)" -ForegroundColor Green
Write-Host ""

# Buscar Launch4j
$launch4j = $null
$paths = @(
    "C:\Program Files\Launch4j\launch4j.exe",
    "C:\Program Files (x86)\Launch4j\launch4j.exe",
    "$PWD\launch4j\launch4j.exe"
)

foreach ($path in $paths) {
    if (Test-Path $path) {
        $launch4j = $path
        break
    }
}

# Descargar Launch4j si no existe
if ($null -eq $launch4j) {
    Write-Host "Launch4j no encontrado. Descargando..." -ForegroundColor Yellow
    
    $launch4jDir = "$PWD\launch4j"
    if (-not (Test-Path $launch4jDir)) {
        New-Item -ItemType Directory -Path $launch4jDir | Out-Null
    }
    
    $zipPath = "$launch4jDir\launch4j.zip"
    # URL directa de SourceForge (sin redirección)
    $url = "https://downloads.sourceforge.net/project/launch4j/launch4j-3/3.50/launch4j-3.50-windows-x64.zip"
    
    Write-Host "Descargando Launch4j desde SourceForge..." -ForegroundColor Yellow
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    
    try {
        $ProgressPreference = 'SilentlyContinue'
        Invoke-WebRequest -Uri $url -OutFile $zipPath -UseBasicParsing -ErrorAction Stop
        
        Write-Host "Verificando archivo descargado..." -ForegroundColor Yellow
        if (-not (Test-Path $zipPath) -or (Get-Item $zipPath).Length -lt 1000000) {
            throw "Archivo descargado es muy pequeño o no existe"
        }
        
        Write-Host "Extrayendo..." -ForegroundColor Yellow
        Expand-Archive -Path $zipPath -DestinationPath $launch4jDir -Force -ErrorAction Stop
        Remove-Item $zipPath -Force
    } catch {
        Write-Host "Error al descargar Launch4j: $_" -ForegroundColor Red
        Write-Host "Por favor, descarga Launch4j manualmente desde: https://launch4j.sourceforge.net/" -ForegroundColor Yellow
        Write-Host "Y extráelo en la carpeta: $launch4jDir" -ForegroundColor Yellow
        Read-Host "Presiona Enter para salir"
        exit 1
    }
    
    # Buscar el ejecutable
    $launch4j = Get-ChildItem -Path $launch4jDir -Filter "launch4j.exe" -Recurse -ErrorAction SilentlyContinue | 
                Select-Object -First 1 -ExpandProperty FullName
}

if ($null -eq $launch4j -or -not (Test-Path $launch4j)) {
    Write-Host "ERROR: No se pudo encontrar o descargar Launch4j" -ForegroundColor Red
    Read-Host "Presiona Enter para salir"
    exit 1
}

Write-Host "[OK] Usando Launch4j: $launch4j" -ForegroundColor Green
Write-Host ""

# Crear directorio en Descargas del disco D
$distDir = "D:\Descargas"
if (-not (Test-Path $distDir)) {
    # Intentar con nombre en inglés
    $distDir = "D:\Downloads"
    if (-not (Test-Path $distDir)) {
        # Crear la carpeta si no existe
        New-Item -ItemType Directory -Path "D:\Descargas" -Force | Out-Null
        $distDir = "D:\Descargas"
    }
}
Write-Host "Directorio de salida: $distDir" -ForegroundColor Cyan

# Crear configuración optimizada
$configPath = "$PWD\launch4j-config-optimized.xml"
$jarPathFull = (Resolve-Path $jarPath).Path
$outfileFull = "$distDir\CONNECTING-POS.exe"
$configContent = @"
<?xml version="1.0" encoding="UTF-8"?>
<launch4jConfig>
  <dontWrapJar>false</dontWrapJar>
  <headerType>gui</headerType>
  <jar>$jarPathFull</jar>
  <outfile>$outfileFull</outfile>
  <errTitle>CONNECTING POS Error</errTitle>
  <cmdLine></cmdLine>
  <chdir>.</chdir>
  <priority>normal</priority>
  <downloadUrl>https://adoptium.net/</downloadUrl>
  <supportUrl>https://github.com/Sebastian245675/punto-mx</supportUrl>
  <stayAlive>false</stayAlive>
  <restartOnCrash>false</restartOnCrash>
  <manifest></manifest>
  <icon></icon>
  <jre>
    <path></path>
    <bundledJre64Bit>false</bundledJre64Bit>
    <bundledJreAsFallback>false</bundledJreAsFallback>
    <minVersion>21</minVersion>
    <maxVersion></maxVersion>
    <jdkPreference>preferJre</jdkPreference>
    <runtimeBits>64/32</runtimeBits>
    <opt>-Djava.io.tmpdir=%TEMP%</opt>
    <opt>-Xms512m</opt>
    <opt>-Xmx2048m</opt>
    <opt>-XX:+UseG1GC</opt>
    <opt>-XX:MaxGCPauseMillis=200</opt>
    <opt>-Dsun.java2d.d3d=false</opt>
    <opt>-Dsun.java2d.opengl=false</opt>
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

$configContent | Out-File -FilePath $configPath -Encoding UTF8

# Generar EXE
Write-Host "Generando EXE..." -ForegroundColor Yellow
& $launch4j $configPath

# Verificar resultado
$exePath = "$distDir\CONNECTING-POS.exe"
if (Test-Path $exePath) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "[OK] EXE generado exitosamente!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    
    $exeSize = (Get-Item $exePath).Length / 1MB
    $exeSizeRounded = [math]::Round($exeSize, 2)
    Write-Host "Tamaño: $exeSizeRounded MB" -ForegroundColor Cyan
    
    if ($exeSize -lt 120) {
        Write-Host "[OK] El EXE es menor a 120 MB - Objetivo cumplido!" -ForegroundColor Green
    } else {
        Write-Host "[ADVERTENCIA] El EXE es mayor a 120 MB" -ForegroundColor Yellow
    }
    
    Write-Host ""
    Write-Host "Ubicación: $exePath" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "NOTA: Este EXE requiere Java 21+ instalado en el sistema." -ForegroundColor Yellow
    Write-Host "Si Java no está instalado, el usuario será dirigido a descargarlo." -ForegroundColor Yellow
    Write-Host ""
    
    # Limpiar
    Remove-Item $configPath -Force -ErrorAction SilentlyContinue
} else {
    Write-Host ""
    Write-Host "ERROR: No se pudo generar el EXE" -ForegroundColor Red
    Write-Host "Verifica los logs de Launch4j para más detalles" -ForegroundColor Red
    Write-Host ""
    Remove-Item $configPath -Force -ErrorAction SilentlyContinue
}

Read-Host "Presiona Enter para salir"

