chcp 65001
call gradle fatjar
cp app/build/libs/configgen.jar configgen.jar
call gradle -PnoPoi fatjar
cp app/build/libs/configgen.jar cfggen.jar
pause