java --enable-preview  -agentlib:native-image-agent=config-merge-dir=./native-image  -jar cfggen.jar -datadir example/config -verify
java --enable-preview  -agentlib:native-image-agent=config-merge-dir=./native-image  -jar cfggen.jar
pause