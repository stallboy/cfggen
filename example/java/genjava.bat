@echo off
setlocal
cd /d "%~dp0"

set "CFGGEN_JAR=%~dp0..\..\app\build\libs\cfggen.jar"
if not exist "%CFGGEN_JAR%" (
    echo Build cfggen first: app\gradlew.bat -p app fatJar
    exit /b 1
)

rem Refresh this example's generated support sources when upgrading the generator.
if exist "configgen\genjava\*.java" del /q "configgen\genjava\*.java"
rem bytes defaults to the versioned blocks format and pack=base.
java --sun-misc-unsafe-memory-access=allow -jar "%CFGGEN_JAR%" -datadir ../config -gen java,own:-noserver,dir:.,builders:../config/builders.txt,configgendir:.,beautifulName -gen bytes,own:-noserver
if errorlevel 1 exit /b %errorlevel%

rem A mod shares the schema and exports only its own records, with the same field filter.
java --sun-misc-unsafe-memory-access=allow -jar "%CFGGEN_JAR%" -datadir ../config_mod -schema ../config -gen bytes,own:-noserver,pack:demo-mod,requires:base,deps:base.bytes
exit /b %errorlevel%
