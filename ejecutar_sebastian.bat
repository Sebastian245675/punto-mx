@echo off
echo =====================================
echo Sebastian POS - Sistema Modernizado
echo =====================================
echo.
echo Iniciando aplicacion...
echo.

cd /d "d:\unicenta-pos\kriolos-opos-app"

java -cp "target\classes;target\dependency\*" com.openbravo.pos.forms.StartPOS

pause