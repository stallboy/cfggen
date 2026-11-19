@echo off
setlocal
cd /d "%~dp0"
call ..\..\app\gradlew.bat run --args=repl --console=plain
exit /b %errorlevel%
