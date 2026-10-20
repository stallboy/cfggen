@echo off
setlocal
cd /d "%~dp0"

set "CFGGEN_JAR=%~dp0..\..\app\build\libs\cfggen.jar"
if not exist "%CFGGEN_JAR%" (
    echo Build cfggen first: app\gradlew.bat -p app fatJar
    exit /b 1
)

if exist "Config\Loader.cs" del /q "Config\Loader.cs"
rem Build dependencies need a plain package; runtime packages keep the XOR example.
java --sun-misc-unsafe-memory-access=allow -jar "%CFGGEN_JAR%" -datadir ../config -gen cs,prefix:D,dir:.,encoding:UTF-8,implicitUsings -gen bytes,dir:obj/base-pack -gen bytes,cipher:xyz
if errorlevel 1 exit /b %errorlevel%

java --sun-misc-unsafe-memory-access=allow -jar "%CFGGEN_JAR%" -datadir ../config_mod -schema ../config -gen bytes,pack:demo-mod,requires:base,deps:obj/base-pack/base.bytes,cipher:xyz
exit /b %errorlevel%

