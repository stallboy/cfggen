@echo off
setlocal
pushd "%~dp0app" || exit /b 1
call gradlew.bat :cfggen-excel-write:fatJar
if errorlevel 1 (
    popd
    exit /b 1
)
popd
copy /B /Y "%~dp0cfgdev\cfggen-excel-write\build\libs\cfggen-excel-write.jar" "%~dp0cfggen-excel-write.jar"
exit /b %ERRORLEVEL%
