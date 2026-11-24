@echo off
setlocal
cd /d "%~dp0" || exit /b 1

call "%~dp0..\cfggen_common.bat" || exit /b 1

rem Refresh this example's generated support sources when upgrading the generator.
if exist "configgen\genjava\*.java" del /q "configgen\genjava\*.java"
rem bytes defaults to the versioned blocks format and pack=base.
%CFGGEN% -datadir ../config -gen java,own:-noserver,dir:.,builders:../config/builders.txt,configgendir:.,beautifulName,jspecify -gen bytes,own:-noserver
if errorlevel 1 exit /b %errorlevel%

rem A mod shares the schema and exports only its own records, with the same field filter.
%CFGGEN% -datadir ../config_mod -schema ../config -gen bytes,own:-noserver,pack:demo-mod,requires:base,deps:base.bytes
exit /b %errorlevel%
