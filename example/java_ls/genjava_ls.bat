rm -rf configgen

call %~dp0..\cfggen_common.bat
%CFGGEN% -langswitchdir ../i18n/langs -gen java,own:-noserver,dir:.,configgendir:. -gen bytes,own:-noserver,schema
