for %%f in (stream.go package.go LoadErrors.go) do if exist config\%%f del /q config\%%f
call %~dp0..\cfggen_common.bat
%CFGGEN% -gen go,dir:. -gen bytes
