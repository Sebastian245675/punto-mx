@echo off
REM Launcher ultra-rápido para KriolOS POS
REM Ejecuta directamente sin ventana adicional

java -Xms256m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true -Xverify:none -XX:TieredStopAtLevel=1 -XX:+TieredCompilation -Dfile.encoding=UTF-8 -jar kriolos-opos-app\target\kriolos-pos.jar

