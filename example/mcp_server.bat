
@echo off
setlocal
set "TOOLS_JAR=%~dp0..\cfggen-tools.jar"
if not exist "%TOOLS_JAR%" (
    echo cfggen-tools.jar not found. Run gentoolsjar.bat from the repository root first. 1>&2
    exit /b 1
)
pushd "%~dp0" || exit /b 1
java -jar "%TOOLS_JAR%" -datadir config -gen mcpserver
set "RESULT=%ERRORLEVEL%"
popd
pause
exit /b %RESULT%
