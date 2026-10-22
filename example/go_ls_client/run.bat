@echo off
setlocal
cd /d "%~dp0"
go run .
exit /b %errorlevel%
