rm Config/Loader.cs

call %~dp0..\cfggen_common.bat
%CFGGEN% -langswitchdir ../i18n/langs -gen cs,prefix:D,dir:.,encoding:UTF-8,servertext -gen bytes

