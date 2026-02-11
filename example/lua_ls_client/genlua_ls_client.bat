rm cfg/mkcfg.lua
rm cfg/mkcfginit.lua

java -jar ../../cfggen.jar -datadir ../config -langswitchdir ../i18n/langs -gen lua,dir:.,emmylua,sharedEmptyTable,shared,mkcfgdir:cfg

