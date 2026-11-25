@echo off
setlocal
cd /d "%~dp0" || exit /b 1

set "TOOLS_JAR=%~dp0..\..\cfggen-tools.jar"
if not exist "%TOOLS_JAR%" (
    echo cfggen-tools.jar not found. Run gentoolsjar.bat from the repository root first. 1>&2
    exit /b 1
)

java -jar "%TOOLS_JAR%" -tool csvencoding,datadir=.
set "RESULT=%ERRORLEVEL%"
pause
exit /b %RESULT%
