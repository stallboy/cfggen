cd /d "%~dp0" || exit /b 1
call "%~dp0..\cfggen_common.bat" || exit /b 1
%CFGGEN% -datadir ../config -i18nfile en.csv -gen i18n,file=en.csv
