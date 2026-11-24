cd /d "%~dp0" || exit /b 1
call "%~dp0..\cfggen_common.bat" || exit /b 1

if exist "configgen\genjava\*.java" del /q "configgen\genjava\*.java"

%CFGGEN% -datadir ../config -langswitchdir ../i18n/langs -gen java,own:-noserver,dir:.,configgendir:.,prefix:Cfg,jspecify -gen bytes,own:-noserver
