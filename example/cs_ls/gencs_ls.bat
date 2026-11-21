call "%~dp0..\require_output_marker.bat" "Config" || exit /b 1

if exist "Config\Loader.cs" del /q "Config\Loader.cs"

call %~dp0..\cfggen_common.bat
%CFGGEN% -langswitchdir ../i18n/langs -gen cs,prefix:D,dir:.,encoding:UTF-8,servertext,implicitUsings -gen bytes

