@echo off
setlocal
cd /d "%~dp0" || exit /b 1

call "%~dp0..\cfggen_common.bat" || exit /b 1

if exist "configgen\genjava\*.java" del /q "configgen\genjava\*.java"
%CFGGEN% -datadir ../config -gen java,own:-noserver,dir:.,builders:../config/builders.txt,configgendir:.,beautifulName -gen bytes,own:-noserver
exit /b %errorlevel%
