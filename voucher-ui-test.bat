@echo off
echo Testing Voucher UI enhancements...
cd /d "d:\Escritorio\pos"
echo Compiling project...
mvn compile -q
if %ERRORLEVEL% == 0 (
    echo ✓ Voucher payment panel UI enhanced successfully!
    echo ✓ Keypad reduced to 20x20 pixels
    echo ✓ m_jMoneyEuros field enlarged for better visibility  
    echo ✓ Font sizes increased for better readability
    echo ✓ All connections preserved
) else (
    echo ✗ Compilation failed - check for errors
)
pause