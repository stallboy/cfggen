rm -f config/stream.go config/LoadErrors.go
java -jar ../../cfggen.jar -datadir ../config -gen go,dir:.,encoding:UTF-8 -gen bytes
