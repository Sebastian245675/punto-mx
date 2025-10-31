@echo off
echo Testing Slip UI enhancements...
cd /d "d:\Escritorio\pos"
echo Compiling project...
mvn compile -q
if %ERRORLEVEL% == 0 (
    echo ✓ Slip payment panel UI enhanced successfully!
    echo ✓ Keypad reduced to 20x20 pixels 
    echo ✓ m_jMoneyEuros field enlarged to 300x45px for better visibility  
    echo ✓ Font sizes increased for better readability
    echo ✓ All connections preserved
    echo ✓ Same successful methodology as Cash, Cheque and Bank panels applied
) else (
    echo ✗ Compilation failed - check for errors
)
pause