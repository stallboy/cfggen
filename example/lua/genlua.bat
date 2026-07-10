@rem rm -rf common
call %~dp0..\cfggen_common.bat
%CFGGEN%  -gen lua,dir:.,emmylua,sharedEmptyTable,shared,mkcfgdir:common
