cd /d "%~dp0" || exit /b 1
call "%~dp0..\cfggen_common.bat" || exit /b 1

for %%f in (stream.go package.go LoadErrors.go) do if exist config\%%f del /q config\%%f
%CFGGEN% -datadir ../config -gen go,dir:. -gen bytes
