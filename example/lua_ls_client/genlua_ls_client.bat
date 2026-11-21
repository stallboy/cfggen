call "%~dp0..\require_output_marker.bat" "cfg" || exit /b 1

rm cfg/mkcfg.lua
rm cfg/mkcfginit.lua

call %~dp0..\cfggen_common.bat
%CFGGEN% -langswitchdir ../i18n/langs -gen lua,dir:.,emmylua,sharedEmptyTable,shared,mkcfgdir:cfg

