# Script PowerShell para convertir kriolos-pos-launcher.bat en .exe
# Requiere: PowerShell 5.1+ o PowerShell Core

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Convirtiendo .bat a .exe" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar que el archivo .bat existe
$batFile = "kriolos-pos-launcher.bat"
if (-not (Test-Path $batFile)) {
    Write-Host "ERROR: No se encuentra el archivo: $batFile" -ForegroundColor Red
    Read-Host "Presiona Enter para salir"
    exit 1
}

# Crear directorio de salida
if (-not (Test-Path "dist")) {
    New-Item -ItemType Directory -Path "dist" | Out-Null
}

# Crear un script VBS que ejecuta el .bat sin mostrar consola
$vbsContent = @"
Set WshShell = CreateObject("WScript.Shell")
WshShell.Run "cmd /c `"$PWD\$batFile`"", 0, False
Set WshShell = Nothing
"@

$vbsFile = "dist\kriolos-pos-launcher.vbs"
$vbsContent | Out-File -FilePath $vbsFile -Encoding ASCII

Write-Host "Script VBS creado: $vbsFile" -ForegroundColor Green
Write-Host ""
Write-Host "NOTA: Para crear un verdadero .exe, puedes:" -ForegroundColor Yellow
Write-Host "  1. Usar Bat To Exe Converter (gratis): https://www.battoexeconverter.com/" -ForegroundColor Yellow
Write-Host "  2. Usar IExpress (incluido en Windows)" -ForegroundColor Yellow
Write-Host "  3. Usar jpackage (ver create-exe.bat)" -ForegroundColor Yellow
Write-Host "  4. Usar Launch4j (ver create-exe-launch4j.bat)" -ForegroundColor Yellow
Write-Host ""
Write-Host "El archivo VBS creado puede ejecutarse sin mostrar consola." -ForegroundColor Green
Write-Host ""

Read-Host "Presiona Enter para salir"

