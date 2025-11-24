chcp 65001
call gradle fatjar
copy /B /Y app\build\libs\configgen.jar cfggen.jar
pause