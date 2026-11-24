@rem rm -rf common
cd /d "%~dp0" || exit /b 1
call "%~dp0..\cfggen_common.bat" || exit /b 1
%CFGGEN% -datadir ../config -gen lua,dir:.,emmylua,sharedEmptyTable,shared,mkcfgdir:common
