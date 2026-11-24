cd /d "%~dp0" || exit /b 1
call "%~dp0..\cfggen_common.bat" || exit /b 1

if exist "Config\Loader.cs" del /q "Config\Loader.cs"

%CFGGEN% -datadir ../config -langswitchdir ../i18n/langs -gen cs,prefix:D,dir:.,encoding:UTF-8,servertext,implicitUsings -gen bytes

