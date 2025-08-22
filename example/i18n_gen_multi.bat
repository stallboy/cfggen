rem java -jar ../cfggen.jar -datadir config -i18nfile i18n/langs/en  -gen i18nbyid,dir=i18n/langs/en,backup=i18n/backup
rem java -jar ../cfggen.jar -datadir config -i18nfile i18n/langs/tw  -gen i18nbyid,dir=i18n/langs/tw,backup=i18n/backup
java -jar ../cfggen.jar -datadir config -langswitchdir i18n/langs  -gen i18nbyid,dir=i18n/langs,backup=i18n/backup
pause
