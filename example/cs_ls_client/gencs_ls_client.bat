rm Config/Loader.cs

call %~dp0..\cfggen_common.bat
%CFGGEN% -langswitchdir ../i18n/langs -gen cs,dir:.,encoding:UTF-8,unity -gen bytes

