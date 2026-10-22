@echo off
setlocal
cd /d "%~dp0"

if exist "Config\Loader.cs" del /q "Config\Loader.cs"

call "%~dp0..\cfggen_common.bat"
%CFGGEN% -langswitchdir ../i18n/langs -gen cs,prefix:D,dir:.,encoding:UTF-8,servertext,implicitUsings -gen bytes
exit /b %errorlevel%

