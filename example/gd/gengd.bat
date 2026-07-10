rm -f config/ConfigStream.gd
rm -f config/ConfigLoader.gd
rm -f config/ConfigErrors.gd
rm -f config/TextPoolManager.gd

call %~dp0..\cfggen_common.bat
%CFGGEN% -gen gd,own:-nogd,dir:config -gen bytes,own:-nogd,dir:.

rm -rf jte-classes