rm -rf configgen

java -jar ../../cfggen.jar -datadir ../config -langswitchdir ../i18n/langs -gen java,own:-noserver,dir:.,sealed,builders:../config/builders.txt,configgendir:. -gen bytes,own:-noserver,schema
