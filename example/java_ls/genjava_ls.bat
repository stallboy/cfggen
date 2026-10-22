@echo off
setlocal
cd /d "%~dp0"
if exist "configgen\genjava\*.java" del /q "configgen\genjava\*.java"

call "%~dp0..\cfggen_common.bat"
%CFGGEN% -langswitchdir ../i18n/langs -gen java,own:-noserver,dir:.,configgendir:.,prefix:Cfg -gen bytes,own:-noserver
exit /b %errorlevel%
