rm -rf configgen

java -jar ../../cfggen.jar -datadir ../config -langswitchdir ../i18n/langs -gen java,own:-noserver,dir:.,configgendir:. -gen bytes,own:-noserver,schema
