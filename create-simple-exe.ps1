# Script para crear un ejecutable simple usando .NET
# Crea un ejecutable único que ejecuta el JAR directamente

$jarPath = "kriolos-opos-app\target\kriolos-pos.jar"
$exePath = "D:\Descargas\KriolOS-POS.exe"

if (-not (Test-Path $jarPath)) {
    Write-Host "ERROR: No se encuentra el JAR: $jarPath" -ForegroundColor Red
    exit 1
}

# Crear código C# para el launcher
$csharpCode = @"
using System;
using System.Diagnostics;
using System.IO;
using System.Reflection;

class KriolOSLauncher {
    static void Main() {
        try {
            string exeDir = Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location);
            string jarPath = Path.Combine(exeDir, "kriolos-pos.jar");
            
            if (!File.Exists(jarPath)) {
                Console.WriteLine("ERROR: No se encuentra kriolos-pos.jar");
                Console.WriteLine("El archivo debe estar en la misma carpeta que el ejecutable.");
                Console.ReadKey();
                return;
            }
            
            ProcessStartInfo psi = new ProcessStartInfo {
                FileName = "java",
                Arguments = $"-Xms256m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true -Xverify:none -XX:TieredStopAtLevel=1 -XX:+TieredCompilation -Dfile.encoding=UTF-8 -jar \"{jarPath}\"",
                UseShellExecute = false,
                CreateNoWindow = true
            };
            
            Process.Start(psi).WaitForExit();
        } catch (Exception ex) {
            Console.WriteLine("ERROR: " + ex.Message);
            Console.ReadKey();
        }
    }
}
"@

# Compilar el launcher
$csharpFile = [System.IO.Path]::GetTempFileName() + ".cs"
$csharpCode | Out-File -FilePath $csharpFile -Encoding UTF8

Write-Host "Compilando launcher..." -ForegroundColor Yellow

# Buscar el compilador de C#
$cscPath = $null
$possiblePaths = @(
    "${env:ProgramFiles}\Microsoft Visual Studio\2022\*\MSBuild\Current\Bin\Roslyn\csc.exe",
    "${env:ProgramFiles(x86)}\Microsoft Visual Studio\2022\*\MSBuild\Current\Bin\Roslyn\csc.exe",
    "${env:ProgramFiles}\Microsoft Visual Studio\2019\*\MSBuild\Current\Bin\Roslyn\csc.exe",
    "${env:ProgramFiles(x86)}\Microsoft Visual Studio\2019\*\MSBuild\Current\Bin\Roslyn\csc.exe",
    "${env:windir}\Microsoft.NET\Framework64\v4.0.30319\csc.exe",
    "${env:windir}\Microsoft.NET\Framework\v4.0.30319\csc.exe"
)

foreach ($path in $possiblePaths) {
    $found = Get-ChildItem -Path $path -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found) {
        $cscPath = $found.FullName
        break
    }
}

if (-not $cscPath) {
    Write-Host "ERROR: No se encontró el compilador de C# (csc.exe)" -ForegroundColor Red
    Write-Host "Intentando método alternativo..." -ForegroundColor Yellow
    
    # Método alternativo: Crear un script .bat optimizado
    $batContent = @"
@echo off
cd /d "%~dp0"
java -Xms256m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true -Xverify:none -XX:TieredStopAtLevel=1 -XX:+TieredCompilation -Dfile.encoding=UTF-8 -jar "%~dp0kriolos-pos.jar"
"@
    
    # Copiar JAR a destino
    if (-not (Test-Path "D:\Descargas")) {
        New-Item -ItemType Directory -Path "D:\Descargas" -Force | Out-Null
    }
    
    Copy-Item $jarPath -Destination "D:\Descargas\kriolos-pos.jar" -Force
    $batContent | Out-File -FilePath "D:\Descargas\KriolOS-POS.bat" -Encoding ASCII
    
    Write-Host "Creado: D:\Descargas\KriolOS-POS.bat" -ForegroundColor Green
    Write-Host "NOTA: Para convertirlo a .exe, usa Bat To Exe Converter:" -ForegroundColor Yellow
    Write-Host "https://www.battoexeconverter.com/" -ForegroundColor Yellow
    
    Remove-Item $csharpFile -ErrorAction SilentlyContinue
    exit 0
}

# Compilar
$output = & $cscPath /target:winexe /out:"$exePath" "$csharpFile" 2>&1

if ($LASTEXITCODE -eq 0) {
    # Copiar JAR junto al ejecutable
    $jarDest = "D:\Descargas\kriolos-pos.jar"
    Copy-Item $jarPath -Destination $jarDest -Force
    
    Write-Host "¡Ejecutable creado exitosamente!" -ForegroundColor Green
    Write-Host "Ubicación: $exePath" -ForegroundColor Cyan
    Write-Host "JAR copiado: $jarDest" -ForegroundColor Cyan
} else {
    Write-Host "ERROR al compilar:" -ForegroundColor Red
    Write-Host $output
}

# Limpiar
Remove-Item $csharpFile -ErrorAction SilentlyContinue

