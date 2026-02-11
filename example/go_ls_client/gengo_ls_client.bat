rm -f config/stream.go config/LoadErrors.go config/Text.go
java -jar ../../cfggen.jar -datadir ../config -langswitchdir ../i18n/langs -gen go,dir:.,encoding:UTF-8 -gen bytes
