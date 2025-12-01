#!/bin/bash

echo "========================================"
echo "Configurando Java..."
echo "========================================"

# Configurar JAVA_HOME para usar Java 25
export JAVA_HOME="C:/Program Files/Java/jdk-25"
export PATH="$JAVA_HOME/bin:$PATH"

echo "JAVA_HOME: $JAVA_HOME"
java -version

echo ""
echo "========================================"
echo "Compilando proyecto (saltando tests)..."
echo "========================================"

# Usar Maven Wrapper si existe, sino usar mvn
if [ -f "./mvnw" ]; then
    ./mvnw clean install -DskipTests
else
    mvn clean install -DskipTests
fi

if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR: La compilación falló"
    exit 1
fi

echo ""
echo "========================================"
echo "Ejecutando aplicación..."
echo "========================================"

java -Xms256m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication \
     -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true \
     -Xverify:none -XX:TieredStopAtLevel=1 -XX:+TieredCompilation \
     -Dfile.encoding=UTF-8 \
     -jar kriolos-opos-app/target/kriolos-pos.jar

