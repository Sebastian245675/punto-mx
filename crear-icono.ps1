# Script para crear un icono simple si no existe
# Requiere .NET Framework

$ErrorActionPreference = "Stop"

Write-Host "Creando icono para CONNECTING POS..." -ForegroundColor Cyan

$iconPath = "icono.ico"

if (Test-Path $iconPath) {
    Write-Host "El archivo icono.ico ya existe." -ForegroundColor Yellow
    $response = Read-Host "¿Deseas sobrescribirlo? (S/N)"
    if ($response -ne "S" -and $response -ne "s") {
        Write-Host "Operación cancelada." -ForegroundColor Yellow
        exit 0
    }
}

try {
    # Cargar System.Drawing
    Add-Type -AssemblyName System.Drawing
    
    # Crear un bitmap de 256x256 (tamaño estándar para iconos)
    $size = 256
    $bitmap = New-Object System.Drawing.Bitmap($size, $size)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    
    # Configurar calidad
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    
    # Fondo azul
    $graphics.Clear([System.Drawing.Color]::FromArgb(33, 150, 243))
    
    # Dibujar un círculo blanco en el centro
    $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
    $pen = New-Object System.Drawing.Pen([System.Drawing.Color]::White, 20)
    
    $margin = 40
    $circleSize = $size - ($margin * 2)
    $graphics.FillEllipse($brush, $margin, $margin, $circleSize, $circleSize)
    $graphics.DrawEllipse($pen, $margin, $margin, $circleSize, $circleSize)
    
    # Dibujar texto "CP" en el centro
    $font = New-Object System.Drawing.Font("Arial", 80, [System.Drawing.FontStyle]::Bold)
    $textBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(33, 150, 243))
    $text = "CP"
    $textSize = $graphics.MeasureString($text, $font)
    $x = ($size - $textSize.Width) / 2
    $y = ($size - $textSize.Height) / 2
    $graphics.DrawString($text, $font, $textBrush, $x, $y)
    
    # Guardar como icono
    # Nota: .NET no tiene soporte directo para .ico, así que guardamos como PNG y convertimos
    $pngPath = "icono-temp.png"
    $bitmap.Save($pngPath, [System.Drawing.Imaging.ImageFormat]::Png)
    
    # Convertir PNG a ICO usando una herramienta externa o método alternativo
    # Por ahora, simplemente renombramos (esto no funcionará como icono real)
    # Para un icono real, necesitarías usar una herramienta como ImageMagick o similar
    
    Write-Host "Icono creado como PNG: $pngPath" -ForegroundColor Yellow
    Write-Host "NOTA: Para crear un .ico real, necesitas convertir el PNG usando:" -ForegroundColor Yellow
    Write-Host "  - ImageMagick: magick convert icono-temp.png icono.ico" -ForegroundColor Yellow
    Write-Host "  - O usar una herramienta online como: https://convertio.co/png-ico/" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Por ahora, puedes usar el PNG como referencia y crear el .ico manualmente." -ForegroundColor Yellow
    
    # Limpiar
    $graphics.Dispose()
    $bitmap.Dispose()
    $brush.Dispose()
    $pen.Dispose()
    $textBrush.Dispose()
    $font.Dispose()
    
} catch {
    Write-Host "ERROR: No se pudo crear el icono: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Alternativas:" -ForegroundColor Yellow
    Write-Host "1. Descarga un icono desde: https://www.flaticon.com/ o similar" -ForegroundColor Yellow
    Write-Host "2. Crea un icono usando: https://www.favicon-generator.org/" -ForegroundColor Yellow
    Write-Host "3. Usa una herramienta como IcoFX o GIMP para crear el icono" -ForegroundColor Yellow
    exit 1
}
