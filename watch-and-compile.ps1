# Script de PowerShell para desarrollo en tiempo real
# Monitorea cambios en archivos .java y recompila automáticamente

$projectPath = "d:\unicenta-pos"
$watchPath = "$projectPath\kriolos-opos-app\src\main\java"

Write-Host "🔄 Iniciando monitoreo de archivos Java en: $watchPath" -ForegroundColor Green
Write-Host "📝 Modifica cualquier archivo .java y se recompilará automáticamente" -ForegroundColor Yellow
Write-Host "⏹️  Presiona Ctrl+C para detener" -ForegroundColor Red
Write-Host ""

# Crear FileSystemWatcher
$watcher = New-Object System.IO.FileSystemWatcher
$watcher.Path = $watchPath
$watcher.Filter = "*.java"
$watcher.IncludeSubdirectories = $true
$watcher.EnableRaisingEvents = $true

# Definir la acción cuando se detecta un cambio
$action = {
    $path = $Event.SourceEventArgs.FullPath
    $changeType = $Event.SourceEventArgs.ChangeType
    $fileName = Split-Path $path -Leaf
    
    Write-Host "🔔 Detectado cambio en: $fileName" -ForegroundColor Cyan
    Write-Host "⚙️  Recompilando..." -ForegroundColor Yellow
    
    # Cambiar al directorio del proyecto
    Set-Location $projectPath
    
    # Ejecutar Maven compile
    $result = mvn clean compile -DskipTests -pl kriolos-opos-app 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Compilación exitosa!" -ForegroundColor Green
        Write-Host "🔄 Listo para próximos cambios..." -ForegroundColor Blue
    } else {
        Write-Host "❌ Error en compilación:" -ForegroundColor Red
        Write-Host $result -ForegroundColor Red
    }
    Write-Host ""
}

# Registrar el evento
Register-ObjectEvent -InputObject $watcher -EventName "Changed" -Action $action

try {
    # Mantener el script corriendo
    while ($true) {
        Start-Sleep 1
    }
} finally {
    # Limpiar cuando se termine
    $watcher.EnableRaisingEvents = $false
    $watcher.Dispose()
    Write-Host "🛑 Monitoreo detenido" -ForegroundColor Red
}