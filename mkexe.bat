REM Step 1: gradle build
call gradle -PnoPoi build
rm -rf exe
mkdir exe
cd exe
cp ../app/build/distributions/app.zip app.zip
unzip app.zip
rm app.zip
rm -rf app/bin

REM Step 1: Use jlink to create a custom JRE
echo Creating custom JRE...
jlink --add-modules java.base,java.logging,java.xml,jdk.httpserver,jdk.unsupported --strip-debug --no-header-files --no-man-pages --output custom-jre

IF %ERRORLEVEL% NEQ 0 (
    echo jlink command failed!
    exit /b %ERRORLEVEL%
)

REM Step 2: Use jpackage to package the application into an exe
echo Creating EXE using jpackage...
jpackage --name cfggen --input app --main-class configgen.gen.Main --main-jar lib/app.jar --runtime-image custom-jre --dest . --win-console --type app-image

IF %ERRORLEVEL% NEQ 0 (
    echo jpackage command failed!
    exit /b %ERRORLEVEL%
)

zip -r cfggen.zip cfggen
cd ..
mv exe/cfggen.zip .

echo EXE creation complete!
pause
