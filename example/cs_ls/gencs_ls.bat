rm Config/Loader.cs

java -jar ../../cfggen.jar -datadir ../config -langswitchdir ../i18n/langs -gen cs,prefix:D,dir:.,encoding:UTF-8,servertext -gen bytes

