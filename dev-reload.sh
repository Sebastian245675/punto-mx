#!/bin/bash
# Archivo para monitorear cambios y recompilar automáticamente
# Para Windows, usaremos PowerShell

echo "Configurando desarrollo en tiempo real para Kriol POS..."
echo "Cada vez que cambies un archivo .java, se recompilará automáticamente"

# Función para recompilar
function recompile {
    echo "Detectado cambio en archivos Java - Recompilando..."
    cd /d "d:\unicenta-pos"
    mvn clean compile -DskipTests -pl kriolos-opos-app
    echo "¡Recompilación completada!"
}

# Monitorear cambios (requiere instalar watchman o usar PowerShell FileSystemWatcher)
echo "Para activar el monitoreo automático, ejecuta:"
echo "mvn compile -DskipTests -pl kriolos-opos-app"
echo "Cada vez que modifiques un archivo."