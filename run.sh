#!/bin/bash

echo "========================================"
echo "Ejecutando aplicación..."
echo "========================================"

if [ ! -f "kriolos-opos-app/target/kriolos-pos.jar" ]; then
    echo "ERROR: El JAR no existe. Por favor compila primero con: ./compile.sh"
    exit 1
fi

java -Xms256m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication \
     -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true \
     -Xverify:none -XX:TieredStopAtLevel=1 -XX:+TieredCompilation \
     -Dfile.encoding=UTF-8 \
     -jar kriolos-opos-app/target/kriolos-pos.jar

