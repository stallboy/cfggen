rm -rf common
java -jar ../../cfggen.jar -datadir ../config  -gen lua,dir:.,emmylua,sharedEmptyTable,shared,mkcfgdir:common
