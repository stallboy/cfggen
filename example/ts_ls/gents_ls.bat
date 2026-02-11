rm ConfigUtil.ts
java -jar ../../cfggen.jar  -datadir ../config -langswitchdir ../i18n/langs -gen ts,servertext -gen bytes
