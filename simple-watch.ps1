# Script simple para monitoreo de cambios
$projectPath = "d:\unicenta-pos"
$watchPath = "$projectPath\kriolos-opos-app\src\main\java"

Write-Host "Iniciando monitoreo de archivos Java..." -ForegroundColor Green
Write-Host "Directorio: $watchPath" -ForegroundColor Yellow
Write-Host "Presiona Ctrl+C para detener" -ForegroundColor Red

# Crear el watcher
$watcher = New-Object System.IO.FileSystemWatcher
$watcher.Path = $watchPath
$watcher.Filter = "*.java"
$watcher.IncludeSubdirectories = $true
$watcher.EnableRaisingEvents = $true

# Acción para cambios
$action = {
    $fileName = Split-Path $Event.SourceEventArgs.FullPath -Leaf
    Write-Host "Cambio detectado en: $fileName" -ForegroundColor Cyan
    Write-Host "Recompilando..." -ForegroundColor Yellow
    
    Set-Location "d:\unicenta-pos"
    mvn compile -DskipTests -pl kriolos-opos-app
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Compilación exitosa!" -ForegroundColor Green
    } else {
        Write-Host "Error en compilación" -ForegroundColor Red
    }
}

# Registrar evento
Register-ObjectEvent -InputObject $watcher -EventName "Changed" -Action $action

# Mantener corriendo
try {
    while ($true) { Start-Sleep 1 }
} finally {
    $watcher.Dispose()
}