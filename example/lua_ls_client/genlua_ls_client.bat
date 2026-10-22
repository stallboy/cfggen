@echo off
setlocal
pushd "%~dp0"
if exist "cfg\mkcfg.lua" del /q "cfg\mkcfg.lua"
if exist "cfg\mkcfginit.lua" del /q "cfg\mkcfginit.lua"

call "%~dp0..\cfggen_common.bat"
%CFGGEN% -langswitchdir ../i18n/langs -gen lua,dir:.,emmylua,sharedEmptyTable,shared,mkcfgdir:cfg
set "result=%errorlevel%"
popd
exit /b %result%

