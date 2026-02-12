rm -f config/ConfigStream.gd
rm -f config/ConfigLoader.gd
rm -f config/ConfigErrors.gd
rm -f config/TextPoolManager.gd

java -jar ../../cfggen.jar -datadir ../config -gen gd,own:-nogd,dir:config -gen bytes,own:-nogd,dir:.

rm -rf jte-classes