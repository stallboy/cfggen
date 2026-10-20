if exist "ConfigUtil.ts" del /q "ConfigUtil.ts"
call %~dp0..\cfggen_common.bat
%CFGGEN% -gen ts -gen bytes
