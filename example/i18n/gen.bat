cd /d "%~dp0" || exit /b 1
call "%~dp0..\cfggen_common.bat" || exit /b 1
%CFGGEN% -datadir ../config -langswitchdir langs  -gen i18nbyid,dir=langs,backup=backup
