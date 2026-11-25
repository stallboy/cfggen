@echo off
setlocal
pushd "%~dp0app" || exit /b 1
call gradlew.bat :fatJar
if errorlevel 1 (
    popd
    exit /b 1
)
popd
copy /B /Y "%~dp0app\build\libs\cfggen.jar" "%~dp0cfggen.jar"
set "RESULT=%ERRORLEVEL%"
pause
exit /b %RESULT%
