# Script para crear ejecutable .exe optimizado (<= 150 MB)
# Usa Launch4j para crear un wrapper ligero alrededor del JAR

$ErrorActionPreference = "Stop"

Write-Host "========================================"
Write-Host "Generador de EXE Optimizado (<= 150 MB)"
Write-Host "========================================"
Write-Host ""

# Configuración
$PROJECT_PATH = $PSScriptRoot
$JAR_PATH = Join-Path $PROJECT_PATH "kriolos-opos-app\target\kriolos-pos.jar"
$OUTPUT_DIR = "D:\Descargas"
if (-not (Test-Path $OUTPUT_DIR)) {
    $OUTPUT_DIR = "$env:USERPROFILE\Downloads"
}
$OUTPUT_EXE = Join-Path $OUTPUT_DIR "La Conchita PDV.exe"
$CONFIG_PATH = Join-Path $PROJECT_PATH "launch4j-config-150mb.xml"
$ICON_PATH = Join-Path $PROJECT_PATH "new_pos_icon.png"

# Configurar JAVA_HOME dinámicamente si no está establecido o verificar su existencia
if (-not $env:JAVA_HOME -or -not (Test-Path $env:JAVA_HOME)) {
    $commonJavaPaths = @(
        "C:\Program Files\Java\jdk-25.0.2",
        "C:\Program Files\Java\jdk-25",
        "C:\Program Files\Java\jdk-21",
        "C:\Program Files\Java\jdk-17"
    )
    foreach ($path in $commonJavaPaths) {
        if (Test-Path $path) {
            $env:JAVA_HOME = $path
            $env:PATH = "$(Join-Path $path 'bin');$env:PATH"
            Write-Host "[INFO] JAVA_HOME configurado automáticamente a: $env:JAVA_HOME"
            break
        }
    }
} else {
    $binPath = Join-Path $env:JAVA_HOME "bin"
    if ($env:PATH -notlike "*$binPath*") {
        $env:PATH = "$binPath;$env:PATH"
    }
}

# Verificar JAR
Write-Host "[1] Verificando JAR compilado..."
if (-not (Test-Path $JAR_PATH)) {
    Write-Host "[ERROR] JAR no encontrado en: $JAR_PATH"
    Write-Host ""
    Write-Host "Compilando proyecto..."
    if (Get-Command "mvn" -ErrorAction SilentlyContinue) {
        & mvn clean package -DskipTests
    } elseif (Test-Path "mvnw.cmd") {
        Write-Host "[INFO] Usando Maven Wrapper (mvnw.cmd) para compilar..."
        & .\mvnw.cmd clean package -DskipTests
    } else {
        Write-Host "[ERROR] No se encontró Maven (mvn) ni el Maven Wrapper (mvnw.cmd)"
        exit 1
    }
    if (-not (Test-Path $JAR_PATH)) {
        Write-Host "[ERROR] No se pudo compilar el JAR"
        exit 1
    }
}

$jarSize = (Get-Item $JAR_PATH).Length / 1MB
Write-Host "[OK] JAR encontrado: $([math]::Round($jarSize, 2)) MB"
Write-Host ""

# Buscar Launch4j
Write-Host "[2] Buscando Launch4j..."
$LAUNCH4J_PATH = $null

$possiblePaths = @(
    "C:\Program Files\Launch4j\launch4jc.exe",
    "C:\Program Files (x86)\Launch4j\launch4jc.exe",
    (Join-Path $PROJECT_PATH "launch4j\launch4jc.exe"),
    (Join-Path $PROJECT_PATH "launch4j-bin\launch4jc.exe")
)

foreach ($path in $possiblePaths) {
    if (Test-Path $path) {
        $LAUNCH4J_PATH = $path
        break
    }
}

# Si no se encuentra, intentar descargar
if (-not $LAUNCH4J_PATH) {
    Write-Host "[INFO] Launch4j no encontrado. Descargando..."
    $launch4jDir = Join-Path $PROJECT_PATH "launch4j"
    if (-not (Test-Path $launch4jDir)) {
        New-Item -ItemType Directory -Path $launch4jDir -Force | Out-Null
    }
    
    $launch4jZip = Join-Path $launch4jDir "launch4j.zip"
    $launch4jUrl = "https://sourceforge.net/projects/launch4j/files/launch4j-3/3.50/launch4j-3.50-windows-x64.zip/download"
    
    try {
        Write-Host "Descargando Launch4j desde SourceForge..."
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -Uri $launch4jUrl -OutFile $launch4jZip -UseBasicParsing
        
        Write-Host "Extrayendo Launch4j..."
        Expand-Archive -Path $launch4jZip -DestinationPath $launch4jDir -Force
        Remove-Item $launch4jZip -Force
        
        # Buscar el ejecutable en la estructura extraída
        $launch4jc = Get-ChildItem -Path $launch4jDir -Filter "launch4jc.exe" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($launch4jc) {
            $LAUNCH4J_PATH = $launch4jc.FullName
        }
    } catch {
        Write-Host "[ERROR] No se pudo descargar Launch4j: $_"
        Write-Host ""
        Write-Host "Por favor, descarga Launch4j manualmente desde:"
        Write-Host "https://sourceforge.net/projects/launch4j/files/launch4j-3/3.50/"
        Write-Host "E instálalo en: C:\Program Files\Launch4j"
        exit 1
    }
}

if (-not $LAUNCH4J_PATH -or -not (Test-Path $LAUNCH4J_PATH)) {
    Write-Host "[ERROR] Launch4j no encontrado"
    Write-Host ""
    Write-Host "Por favor, descarga Launch4j desde:"
    Write-Host "https://sourceforge.net/projects/launch4j/files/launch4j-3/3.50/"
    Write-Host "E instálalo en: C:\Program Files\Launch4j"
    exit 1
}

Write-Host "[OK] Launch4j encontrado: $LAUNCH4J_PATH"
Write-Host ""

# Crear directorio de salida
if (-not (Test-Path $OUTPUT_DIR)) {
    New-Item -ItemType Directory -Path $OUTPUT_DIR -Force | Out-Null
}

# Crear configuración de Launch4j
Write-Host "[3] Creando configuración de Launch4j..."

# Buscar icono ICO (Launch4j requiere formato ICO, no PNG)
$iconPath = ""
$iconIco = Join-Path $PROJECT_PATH "new_pos_icon.ico"
if (Test-Path $iconIco) {
    $iconPath = $iconIco
}

# Obtener ruta absoluta del JAR
$jarAbsolutePath = (Resolve-Path $JAR_PATH).Path

# Crear XML de configuración
$configXml = @"
<?xml version="1.0" encoding="UTF-8"?>
<launch4jConfig>
  <dontWrapJar>false</dontWrapJar>
  <headerType>gui</headerType>
  <jar>$jarAbsolutePath</jar>
  <outfile>$OUTPUT_EXE</outfile>
  <errTitle>La Conchita PDV Error</errTitle>
  <cmdLine></cmdLine>
  <chdir>.</chdir>
  <priority>normal</priority>
  <downloadUrl>https://adoptium.net/</downloadUrl>
  <supportUrl>https://github.com/Sebastian245675/punto-mx</supportUrl>
  <stayAlive>false</stayAlive>
  <restartOnCrash>false</restartOnCrash>
  <manifest></manifest>
  <icon>$iconPath</icon>
  <jre>
    <path>%JAVA_HOME%\bin</path>
    <requiresJdk>false</requiresJdk>
    <requires64Bit>false</requires64Bit>
    <minVersion>21</minVersion>
    <maxVersion></maxVersion>
    <opt>-Xms256m</opt>
    <opt>-Xmx2g</opt>
    <opt>-XX:+UseG1GC</opt>
    <opt>-XX:+UseStringDeduplication</opt>
    <opt>-Dsun.java2d.d3d=false</opt>
    <opt>-Dsun.java2d.noddraw=true</opt>
    <opt>-Dsun.java2d.uiScale=1.0</opt>
    <opt>-Xverify:none</opt>
    <opt>-XX:TieredStopAtLevel=1</opt>
    <opt>-XX:+TieredCompilation</opt>
    <opt>-Dfile.encoding=UTF-8</opt>
    <opt>--add-exports java.desktop/sun.swing=ALL-UNNAMED</opt>
    <opt>--add-opens java.desktop/sun.swing=ALL-UNNAMED</opt>
  </jre>
  <versionInfo>
    <fileVersion>1.0.0.0</fileVersion>
    <txtFileVersion>1.0.0</txtFileVersion>
    <fileDescription>La Conchita PDV - Sistema de Punto de Venta</fileDescription>
    <copyright>2026 La Conchita PDV</copyright>
    <productVersion>1.0.0.0</productVersion>
    <txtProductVersion>1.0.0</txtProductVersion>
    <productName>La Conchita PDV</productName>
    <companyName>La Conchita PDV</companyName>
    <internalName>La Conchita PDV</internalName>
    <originalFilename>La Conchita PDV.exe</originalFilename>
  </versionInfo>
</launch4jConfig>
"@

$configXml | Out-File -FilePath $CONFIG_PATH -Encoding UTF8
Write-Host "[OK] Configuración creada: $CONFIG_PATH"
Write-Host ""

# Generar ejecutable
Write-Host "[4] Generando ejecutable con Launch4j..."
Write-Host "Esto puede tardar unos segundos..."
Write-Host ""

& $LAUNCH4J_PATH $CONFIG_PATH

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Error al generar el ejecutable"
    exit 1
}

# Verificar resultado
Write-Host ""
Write-Host "[5] Verificando resultado..."

if (Test-Path $OUTPUT_EXE) {
    $exeSize = (Get-Item $OUTPUT_EXE).Length / 1MB
    $exeSizeRounded = [math]::Round($exeSize, 2)
    
    Write-Host ""
    Write-Host "========================================"
    Write-Host "[OK] EXE GENERADO EXITOSAMENTE"
    Write-Host "========================================"
    Write-Host "Ubicación: $OUTPUT_EXE"
    Write-Host "Tamaño: $exeSizeRounded MB"
    
    if ($exeSize -lt 150) {
        Write-Host "Estado: OK - Dentro del limite de 150 MB"
    } else {
        Write-Host "Estado: ADVERTENCIA - Excede 150 MB (pero se genero correctamente)"
    }
    Write-Host "========================================"
    Write-Host ""
    Write-Host "El ejecutable está listo para usar."
    Write-Host "Nota: Requiere Java 21+ instalado en el sistema."
    Write-Host ""
} else {
    Write-Host "[ERROR] El ejecutable no se generó correctamente"
    exit 1
}

# Limpiar archivo de configuración temporal
if (Test-Path $CONFIG_PATH) {
    Remove-Item $CONFIG_PATH -Force
}

