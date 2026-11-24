@echo off
setlocal
cd /d "%~dp0" || exit /b 1

call "%~dp0..\cfggen_common.bat" || exit /b 1

%CFGGEN% -tool csvencoding,datadir=.
pause
