java -jar ../cfggen.jar -datadir config -langswitchdir i18n -gen cs,dir:cs_ls,encoding:UTF-8 -gen bytes,file=cs_ls/config.bytes

set DIR=cs_ls\Config
if not exist %DIR% mkdir %DIR%

set SDIR=../app/src/main/resources/support
cp %SDIR%/CSV.cs %DIR%
cp %SDIR%/CSVLoader.cs %DIR%
cp %SDIR%/KeyedList.cs %DIR%
cp %SDIR%/LoadErrors.cs %DIR%

pause
