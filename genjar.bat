chcp 65001
call gradle -PnoPoi fatjar
cp app/build/libs/configgen.jar cfggen.jar
pause