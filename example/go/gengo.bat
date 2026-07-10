@rem rm -f config/stream.go config/LoadErrors.go
call %~dp0..\cfggen_common.bat
%CFGGEN% -gen go,dir:.,encoding:UTF-8 -gen bytes
