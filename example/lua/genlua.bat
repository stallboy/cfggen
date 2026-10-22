@echo off
setlocal
pushd "%~dp0"
call "%~dp0..\cfggen_common.bat"
%CFGGEN%  -gen lua,dir:.,emmylua,sharedEmptyTable,shared,mkcfgdir:common
set "result=%errorlevel%"
popd
exit /b %result%
