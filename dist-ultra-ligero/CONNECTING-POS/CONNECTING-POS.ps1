# Launcher para CONNECTING POS
$jarPath = Join-Path $PSScriptRoot "kriolos-pos.jar"

# Verificar Java
try {
    $javaVersion = java -version 2>&1
    if ($LASTEXITCODE -ne 0) {
        [System.Windows.Forms.MessageBox]::Show(
            "Java no encontrado.

Por favor instala Java 21 o superior desde:
https://adoptium.net/",
            "CONNECTING POS - Error",
            [System.Windows.Forms.MessageBoxButtons]::OK,
            [System.Windows.Forms.MessageBoxIcon]::Error
        )
        exit 1
    }
} catch {
    [System.Windows.Forms.MessageBox]::Show(
        "Java no encontrado.

Por favor instala Java 21 o superior desde:
https://adoptium.net/",
        "CONNECTING POS - Error",
        [System.Windows.Forms.MessageBoxButtons]::OK,
        [System.Windows.Forms.MessageBoxIcon]::Error
    )
    exit 1
}

# Ejecutar aplicaciÃ³n
Set-Location $PSScriptRoot
java -Xms256m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true -Xverify:none -XX:TieredStopAtLevel=1 -XX:+TieredCompilation -Dfile.encoding=UTF-8 -jar "kriolos-pos.jar"
