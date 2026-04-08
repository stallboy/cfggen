rm Config/Loader.cs

java -jar ../../cfggen.jar -datadir ../config  -gen cs,prefix:D,dir:.,encoding:UTF-8 -gen bytes,cipher=xyz

