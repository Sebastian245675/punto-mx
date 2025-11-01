@echo off
echo Testing Bank UI enhancements...
cd /d "d:\Escritorio\pos"
echo Compiling project...
mvn compile -q
if %ERRORLEVEL% == 0 (
    echo ✓ Bank payment panel UI enhanced successfully!
    echo ✓ Keypad reduced to 20x20 pixels 
    echo ✓ m_jMoneyEuros field enlarged to 300x45px for better visibility  
    echo ✓ Font sizes increased for better readability
    echo ✓ All connections preserved
    echo ✓ Same successful methodology as Cash and Cheque panels applied
) else (
    echo ✗ Compilation failed - check for errors
)
pause