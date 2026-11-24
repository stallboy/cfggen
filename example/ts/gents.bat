cd /d "%~dp0" || exit /b 1
call "%~dp0..\cfggen_common.bat" || exit /b 1

if exist "ConfigUtil.ts" del /q "ConfigUtil.ts"
%CFGGEN% -datadir ../config -gen ts -gen bytes
