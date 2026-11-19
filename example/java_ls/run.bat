@echo off
setlocal
cd /d "%~dp0"
call ..\..\app\gradlew.bat run --console=plain
exit /b %errorlevel%
