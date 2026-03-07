@rem rm Config/Loader.cs
@rem rm Config/KeyedList.cs
@rem rm Config/LoadErrors.cs

java -jar ../../cfggen.jar -datadir ../config  -gen cs,dir:.,encoding:UTF-8 -gen bytes,cipher=xyz

