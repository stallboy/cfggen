@echo off
setlocal
cd /d "%~dp0"
call ..\..\app\gradlew.bat build --console=plain
if errorlevel 1 exit /b %errorlevel%
java -jar build/libs/java.jar
