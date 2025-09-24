java -jar ../cfggen.jar -datadir config -gen java,own:-noserver,dir:java,sealed,builders:config/builders.txt -gen javadata,own:-noserver

set DIR=java\configgen\genjava
if not exist %DIR% mkdir %DIR%

set SDIR=../app/src/main/java/configgen/genjava
cp %SDIR%/Schema.java %DIR%
cp %SDIR%/SchemaBean.java %DIR%
cp %SDIR%/SchemaCompatibleException.java %DIR%
cp %SDIR%/SchemaEnum.java %DIR%
cp %SDIR%/SchemaInterface.java %DIR%
cp %SDIR%/SchemaList.java %DIR%
cp %SDIR%/SchemaMap.java %DIR%
cp %SDIR%/SchemaPrimitive.java %DIR%
cp %SDIR%/SchemaRef.java %DIR%

cp %SDIR%/BinaryToText.java %DIR%
cp %SDIR%/ConfigErr.java %DIR%
cp %SDIR%/ConfigInput.java %DIR%
cp %SDIR%/ConfigOutput.java %DIR%

pause
