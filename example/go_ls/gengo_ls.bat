for %%f in (stream.go package.go LoadErrors.go) do if exist config\%%f del /q config\%%f
rm -f config/stream.go config/LoadErrors.go config/Text.go
call %~dp0..\cfggen_common.bat
%CFGGEN% -langswitchdir ../i18n/langs -gen go,dir:.,serverText -gen bytes
