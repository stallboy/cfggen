rm Config/Loader.cs
rm Config/KeyedList.cs
rm Config/LoadErrors.cs

java -jar ../../cfggen.jar -datadir ../config  -gen cs,dir:.,encoding:UTF-8 -gen bytes,cipher=xyz

