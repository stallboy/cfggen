@echo off
setlocal
set "MAIN_JAR=%~dp0cfggen.jar"
set "TOOLS_JAR=%~dp0cfgdev\cfggen-tools\build\libs\cfggen-tools.jar"
set "EXCEL_JAR=%~dp0cfgdev\cfggen-excel-write\build\libs\cfggen-excel-write.jar"
if not exist "%MAIN_JAR%" (
    echo cfggen.jar not found in the repository root. Run genjar.bat first. 1>&2
    exit /b 1
)

pushd "%~dp0app" || exit /b 1
call gradlew.bat :cfggen-tools:fatJar :cfggen-excel-write:fatJar
if errorlevel 1 (
    popd
    echo Failed to build cfggen-tools.jar. See the Gradle error above. 1>&2
    exit /b 1
)
popd

set "JAVA_EXE=java.exe"
if defined JAVA_HOME set "JAVA_EXE=%JAVA_HOME:"=%\bin\java.exe"
"%JAVA_EXE%" -cp "%MAIN_JAR%;%TOOLS_JAR%;%EXCEL_JAR%" configgen.tool.ToolsMain -h >nul
if errorlevel 1 (
    echo Startup check failed with the existing cfggen.jar. Check that it is compatible with the tools. The new tool JAR was not copied. 1>&2
    exit /b 1
)
copy /B /Y "%EXCEL_JAR%" "%~dp0cfggen-excel-write.jar" || exit /b 1
copy /B /Y "%TOOLS_JAR%" "%~dp0cfggen-tools.jar"
exit /b %ERRORLEVEL%
