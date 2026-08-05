# Script to convert the specified JPEG to ICO and replace internal PNG icons
Add-Type -AssemblyName System.Drawing
$inputPath = Join-Path $PSScriptRoot "WhatsApp Image 2026-04-25 at 10.55.01 AM.jpeg"
$icoPath = Join-Path $PSScriptRoot "app_icon.ico"

if (Test-Path $inputPath) {
    Write-Host "Processing $inputPath..."
    $bmp = New-Object System.Drawing.Bitmap($inputPath)
    
    # 1. Create ICO for the EXE
    Write-Host "Creating ICO: $icoPath"
    $resized = New-Object System.Drawing.Bitmap($bmp, 256, 256)
    $hIcon = $resized.GetHicon()
    $icon = [System.Drawing.Icon]::FromHandle($hIcon)
    $fs = [System.IO.FileStream]::new($icoPath, [System.IO.FileMode]::Create)
    $icon.Save($fs)
    $fs.Close()
    $icon.Dispose()
    $resized.Dispose()

    # 2. Update Internal PNG Icons for Taskbar/Window
    $pngPaths = @(
        "$PSScriptRoot\..\kriolos-opos-assets-image\src\main\resources\com\openbravo\images\connecting_pos_icon.png",
        "$PSScriptRoot\..\kriolos-opos-assets-image\src\main\resources\com\openbravo\images\app_logo_48x48.png",
        "$PSScriptRoot\..\kriolos-opos-assets-image\target\classes\com\openbravo\images\connecting_pos_icon.png",
        "$PSScriptRoot\..\kriolos-opos-assets-image\target\classes\com\openbravo\images\app_logo_48x48.png"
    )

    foreach ($path in $pngPaths) {
        $dir = Split-Path $path
        if (-not (Test-Path $dir)) { 
            Write-Host "Creating directory: $dir"
            New-Item -ItemType Directory -Path $dir -Force | Out-Null
        }
        Write-Host "Updating PNG: $path"
        $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    }

    $bmp.Dispose()
    Write-Host "All icons updated successfully!"
} else {
    Write-Host "Error: Input image not found at $inputPath" -ForegroundColor Red
    exit 1
}
