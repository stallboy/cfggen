rm Config/Loader.cs
rm Config/KeyedList.cs
rm Config/LoadErrors.cs

java -jar ../../cfggen.jar -datadir ../config -langswitchdir ../i18n/langs -gen cs,dir:.,encoding:UTF-8,servertext -gen bytes

