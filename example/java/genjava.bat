rm -rf configgen/
java -jar ../../cfggen.jar -datadir ../config -gen java,own:-noserver,dir:.,sealed,builders:../config/builders.txt,configgendir:. -gen bytes,own:-noserver,schema
