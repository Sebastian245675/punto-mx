# Crear ejecutable .exe FINAL y FUNCIONAL

$jpackage = "C:\Program Files\Java\jdk-24\bin\jpackage.exe"
$jarPath = "kriolos-opos-app\target\kriolos-pos.jar"
$outputDir = "D:\descargas"

Write-Host "Creando ejecutable .exe..."

if (-not (Test-Path $jarPath)) {
    Write-Host "ERROR: JAR no encontrado"
    exit 1
}

if (-not (Test-Path $jpackage)) {
    Write-Host "ERROR: jpackage no encontrado"
    exit 1
}

# Eliminar versión anterior si existe
if (Test-Path "$outputDir\CONNECTING-POS-Simple") {
    Remove-Item "$outputDir\CONNECTING-POS-Simple" -Recurse -Force
}

# Crear ejecutable
& $jpackage --type app-image `
    --name "CONNECTING-POS-Simple" `
    --app-version "1.0.0" `
    --description "CONNECTING POS" `
    --vendor "CONNECTING POS" `
    --input "kriolos-opos-app\target" `
    --main-jar "kriolos-pos.jar" `
    --main-class "com.openbravo.pos.forms.StartPOS" `
    --java-options "-Xms256m" `
    --java-options "-Xmx2g" `
    --java-options "-XX:+UseG1GC" `
    --java-options "-Dsun.java2d.d3d=false" `
    --java-options "-Dsun.java2d.noddraw=true" `
    --java-options "-Xverify:none" `
    --java-options "-XX:TieredStopAtLevel=1" `
    --java-options "-Dfile.encoding=UTF-8" `
    --dest $outputDir

if (Test-Path "$outputDir\CONNECTING-POS-Simple\CONNECTING-POS-Simple.exe") {
    Write-Host ""
    Write-Host "========================================"
    Write-Host "✅ EJECUTABLE CREADO"
    Write-Host "========================================"
    Write-Host ""
    Write-Host "Ubicación: $outputDir\CONNECTING-POS-Simple\CONNECTING-POS-Simple.exe"
    Write-Host ""
    Write-Host "Para usar solo el .exe:"
    Write-Host "  1. Copia CONNECTING-POS-Simple.exe a donde quieras"
    Write-Host "  2. Asegúrate de tener la carpeta 'app' y 'runtime' en la misma ubicación"
    Write-Host "  3. O ejecuta desde la carpeta completa"
    Write-Host ""
} else {
    Write-Host "ERROR: No se pudo crear el ejecutable"
}

