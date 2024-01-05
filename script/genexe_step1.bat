mkdir native-image
java -agentlib:native-image-agent=config-merge-dir=native-image -jar ../cfggen.jar -datadir ../example/config -verify
java -agentlib:native-image-agent=config-merge-dir=native-image -jar ../cfggen.jar
java -agentlib:native-image-agent=config-merge-dir=native-image -jar ../cfggen.jar -datadir ../example/config -gen server

pause