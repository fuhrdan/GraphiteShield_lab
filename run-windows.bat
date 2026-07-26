@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
    echo Java 17 or newer is required.
    echo Install Java and ensure java is available in PATH.
    pause
    exit /b 1
)
java -jar GraphiteShield-Lab.jar
endlocal
