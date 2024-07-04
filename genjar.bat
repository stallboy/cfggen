chcp 65001
call gradle -PnoPoi fatjar
copy /B /Y app\build\libs\configgen.jar cfggen.jar
pause