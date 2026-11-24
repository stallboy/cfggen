@echo off
setlocal
cd /d "%~dp0" || exit /b 1

call "%~dp0..\cfggen_common.bat" || exit /b 1

if exist "Config\Loader.cs" del /q "Config\Loader.cs"
rem Build dependencies need a plain package; runtime packages keep the XOR example.
%CFGGEN% -datadir ../config -gen cs,prefix:D,dir:.,encoding:UTF-8,implicitUsings -gen bytes,dir:obj/base-pack -gen bytes,cipher:xyz
if errorlevel 1 exit /b %errorlevel%

%CFGGEN% -datadir ../config_mod -schema ../config -gen bytes,pack:demo-mod,requires:base,deps:obj/base-pack/base.bytes,cipher:xyz
exit /b %errorlevel%

