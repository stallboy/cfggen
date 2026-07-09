cd app
call gradlew.bat fatjar
cd ..
copy /B /Y app\build\libs\cfggen.jar cfggen.jar
pause