java -jar ../cfggen.jar -datadir config  -gen cs,dir:cs,encoding:UTF-8 -gen bytes,file=cs/config.bytes,cipher:xyz

set DIR=cs\Config
if not exist %DIR% mkdir %DIR%

set SDIR=../app/src/main/resources/support
cp %SDIR%/CSV.cs %DIR%
cp %SDIR%/CSVLoader.cs %DIR%
cp %SDIR%/KeyedList.cs %DIR%
cp %SDIR%/LoadErrors.cs %DIR%

pause
