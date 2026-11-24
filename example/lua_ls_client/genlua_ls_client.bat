cd /d "%~dp0" || exit /b 1
call "%~dp0..\cfggen_common.bat" || exit /b 1

rm cfg/mkcfg.lua
rm cfg/mkcfginit.lua

%CFGGEN% -datadir ../config -langswitchdir ../i18n/langs -gen lua,dir:.,emmylua,sharedEmptyTable,shared,mkcfgdir:cfg

