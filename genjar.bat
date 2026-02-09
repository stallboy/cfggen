cd app
call gradle fatjar
cd ..
copy /B /Y app\build\libs\cfggen.jar cfggen.jar
pause