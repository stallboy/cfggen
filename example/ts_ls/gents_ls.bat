rm ConfigUtil.ts
call %~dp0..\cfggen_common.bat
%CFGGEN% -langswitchdir ../i18n/langs -gen ts,servertext -gen bytes
