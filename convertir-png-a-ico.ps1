# Script para convertir PNG a ICO
# Requiere ImageMagick o usa metodo alternativo

$ErrorActionPreference = "Stop"

Write-Host "Convirtiendo PNG a ICO..." -ForegroundColor Cyan

# Buscar archivos PNG que podrian ser iconos
$pngFiles = @(
    "new_pos_icon.png",
    "icono.png",
    "icon.png"
)

$pngFile = $null
foreach ($file in $pngFiles) {
    if (Test-Path $file) {
        $pngFile = $file
        break
    }
}

if (-not $pngFile) {
    Write-Host "ERROR: No se encontro archivo PNG para convertir" -ForegroundColor Red
    Write-Host ""
    Write-Host "Coloca un archivo PNG llamado 'new_pos_icon.png' o 'icono.png' en la raiz del proyecto" -ForegroundColor Yellow
    exit 1
}

Write-Host "PNG encontrado: $pngFile" -ForegroundColor Green

$icoFile = "icono.ico"

# Intentar usar ImageMagick si esta disponible
$imageMagick = $null
$magickPaths = @(
    "C:\Program Files\ImageMagick-*\magick.exe",
    "C:\Program Files (x86)\ImageMagick-*\magick.exe"
)

foreach ($path in $magickPaths) {
    $found = Get-ChildItem -Path $path -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found) {
        $imageMagick = $found.FullName
        break
    }
}

if ($imageMagick) {
    Write-Host "Usando ImageMagick: $imageMagick" -ForegroundColor Green
    Write-Host "Convirtiendo $pngFile a $icoFile..." -ForegroundColor Yellow
    
    & $imageMagick convert $pngFile -resize 256x256 $icoFile
    
    if (Test-Path $icoFile) {
        Write-Host "Icono creado exitosamente: $icoFile" -ForegroundColor Green
        exit 0
    }
}

# Metodo alternativo: usar PowerShell para crear un ICO basico
Write-Host "ImageMagick no encontrado. Usando metodo alternativo..." -ForegroundColor Yellow

try {
    Add-Type -AssemblyName System.Drawing
    
    # Cargar imagen PNG
    $bitmap = New-Object System.Drawing.Bitmap($pngFile)
    
    # Crear icono en diferentes tamanos (16, 32, 48, 256)
    $iconSizes = @(16, 32, 48, 256)
    $iconImages = New-Object System.Collections.ArrayList
    
    foreach ($size in $iconSizes) {
        $resized = New-Object System.Drawing.Bitmap($bitmap, $size, $size)
        $iconImages.Add($resized) | Out-Null
    }
    
    # Guardar como ICO
    # Nota: .NET no tiene soporte nativo para ICO multi-resolucion
    # Guardamos el tamanio mas grande (256x256) como ICO simple
    $largest = $iconImages[$iconImages.Count - 1]
    
    # Crear icono desde el bitmap mas grande
    $hIcon = $largest.GetHicon()
    $icon = [System.Drawing.Icon]::FromHandle($hIcon)
    
    # Guardar usando FileStream
    $fs = New-Object System.IO.FileStream($icoFile, [System.IO.FileMode]::Create)
    $icon.Save($fs)
    $fs.Close()
    
    # Limpiar
    $icon.Dispose()
    foreach ($img in $iconImages) {
        $img.Dispose()
    }
    $bitmap.Dispose()
    
    if (Test-Path $icoFile) {
        Write-Host "Icono creado exitosamente: $icoFile" -ForegroundColor Green
        Write-Host "NOTA: Este icono solo tiene una resolucion (256x256)" -ForegroundColor Yellow
        Write-Host "Para un icono multi-resolucion, instala ImageMagick:" -ForegroundColor Yellow
        Write-Host "  winget install ImageMagick.ImageMagick" -ForegroundColor Yellow
        exit 0
    }
    
} catch {
    Write-Host "ERROR: No se pudo convertir el PNG a ICO: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Soluciones:" -ForegroundColor Yellow
    Write-Host "1. Instala ImageMagick: winget install ImageMagick.ImageMagick" -ForegroundColor Yellow
    Write-Host "2. Usa una herramienta online: https://convertio.co/png-ico/" -ForegroundColor Yellow
    Write-Host "3. Usa IcoFX o GIMP para crear el icono manualmente" -ForegroundColor Yellow
    exit 1
}
