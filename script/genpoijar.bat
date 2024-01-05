chcp 65001
cd ..
call gradle fatjar
cp app/build/libs/configgen.jar configgen.jar
pause